package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.crawling.instagram.dto.ContentRawData;
import com.ssafy.spark.domain.crawling.instagram.entity.Content;
import com.ssafy.spark.domain.crawling.instagram.entity.File;
import com.ssafy.spark.domain.crawling.instagram.entity.PlatformAccount;
import com.ssafy.spark.domain.crawling.instagram.repository.ContentRepository;
import com.ssafy.spark.domain.crawling.instagram.repository.PlatformAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContentService extends BaseApifyService {

  private final ContentRepository contentRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final S3Service s3Service;
  private final ImageService imageService;

  /**
   * 해당 사용자의 컨텐츠 수집.
   */
  public CompletableFuture<String> getContentsByUsername(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("콘텐츠 수집 시작: {}", username);

        PlatformAccount platformAccount = platformAccountRepository
            .findByAccountNickname(username)
            .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다: " + username));

        log.info("등록된 사용자 확인됨 - Platform Account ID: {}", platformAccount.getId());

        String actorId = "apify~instagram-post-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsLimit", 10); // TODO : 총 몇개의 컨텐츠를 가져올건지
        input.put("skipPinnedPosts", false);
        input.put("username", new String[]{username});

        log.info("콘텐츠 크롤링 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("콘텐츠 수집 실행 - Run ID: {}", runId);

        return waitAndGetContentResults(runId, platformAccount);

      } catch (Exception e) {
        log.error("콘텐츠 수집 중 오류: ", e);
        return "{\"error\": \"콘텐츠 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  private String waitAndGetContentResults(String runId, PlatformAccount platformAccount) {
    try {
      log.info("콘텐츠 결과 대기 시작 - Run ID: {}", runId);

      boolean isCompleted = false;
      int maxAttempts = 30;
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("콘텐츠 실행 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("콘텐츠 수집 실패: {}", status);
          return "{\"error\": \"콘텐츠 수집 실패: " + status + "\"}";
        }

        Thread.sleep(10000);
      }

      if (isCompleted) {
        String jsonResults = getRunResults(runId);
        log.info("콘텐츠 수집 완료 - 데이터 크기: {} bytes", jsonResults.length());

        // 크롤링한 데이터 파싱하고 db와 s3에 저장
        parseAndSaveContents(jsonResults, platformAccount);

        return jsonResults;
      } else {
        String errorMessage = String.format("콘텐츠 수집 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        return "{\"error\": \"" + errorMessage + "\"}";
      }

    } catch (Exception e) {
      log.error("콘텐츠 결과 대기 중 오류: ", e);
      return "{\"error\": \"콘텐츠 결과 대기 중 오류: " + e.getMessage() + "\"}";
    }
  }

  private void parseAndSaveContents(String jsonResults, PlatformAccount platformAccount) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);

      if (results.size() == 0) {
        log.warn("수집된 콘텐츠가 없습니다.");
        return;
      }

      log.info("등록된 사용자 {}의 콘텐츠 {}개 처리 시작",
          platformAccount.getExternalAccountId(), results.size());

      for (JsonNode content : results) {
        try {
          //db 저장
          Content savedContent = saveContentToDatabase(content, platformAccount);

          //s3 저장 (savedContent 정보 활용)
          saveContentToS3(content, platformAccount, savedContent);

          log.info("콘텐츠 저장 완료 - Content ID: {}, External ID: {}",
              savedContent.getId(), content.path("id").asText());

        } catch (Exception e) {
          log.error("개별 콘텐츠 저장 실패: {}", content.path("id").asText(), e);
        }
      }

    } catch (Exception e) {
      log.error("콘텐츠 파싱 및 저장 중 오류: ", e);
    }
  }

  private Content saveContentToDatabase(JsonNode contentNode, PlatformAccount platformAccount) {
    String type = contentNode.path("type").asText();
    String contentTypeId = "Sidecar".equals(type) ? "POST" : "VIDEO_SHORT";

    String externalContentId = contentNode.path("id").asText();

    // 기존 콘텐츠 조회
    Optional<Content> existingContent = contentRepository.findByExternalContentId(externalContentId);

    if (existingContent.isPresent()) {
      // 기존 콘텐츠가 있으면 metrics만 업데이트
      Content content = existingContent.get();

      Long likesCount = contentNode.path("likesCount").asLong(0);
      Long commentsCount = contentNode.path("commentsCount").asLong(0);
      Long viewsCount = contentNode.path("videoViewCount").asLong(0);
      if (viewsCount == 0) {
        viewsCount = contentNode.path("videoPlayCount").asLong(0);
      }
// 음수 값 보정
      if (likesCount < 0) likesCount = 0L;
      if (commentsCount < 0) commentsCount = 0L;
      if (viewsCount < 0) viewsCount = 0L;

      content.setTotalViews(viewsCount);
      content.setTotalLikes(likesCount);
      content.setTotalComments(commentsCount);
      content.setSnapshotDate(LocalDate.now());
      content.setUpdatedAt(LocalDateTime.now());

      log.info("기존 콘텐츠 업데이트: {}", externalContentId);
      return contentRepository.save(content);
    } else {
      // 새 콘텐츠면 전체 저장
      String caption = contentNode.path("caption").asText();
      String url = contentNode.path("url").asText();
      String timestampStr = contentNode.path("timestamp").asText();
      String displayUrl = contentNode.path("displayUrl").asText();

      JsonNode hashtagsNode = contentNode.path("hashtags");
      List<String> hashtags = new ArrayList<>();
      if (hashtagsNode.isArray()) {
        for (JsonNode hashtag : hashtagsNode) {
          hashtags.add(hashtag.asText());
        }
      }

      String cleanDescription = removeHashtagsFromText(caption, hashtags);
      String title = extractTitle(cleanDescription);

      Long likesCount = contentNode.path("likesCount").asLong(0);
      Long commentsCount = contentNode.path("commentsCount").asLong(0);
      Long viewsCount = contentNode.path("videoViewCount").asLong(0);
      if (viewsCount == 0) {
        viewsCount = contentNode.path("videoPlayCount").asLong(0);
      }

      Integer durationSeconds = null;
      if (contentNode.has("videoDuration")) {
        durationSeconds = (int) Math.round(contentNode.path("videoDuration").asDouble());
      }

      LocalDateTime publishedAt = null;
      if (!timestampStr.isEmpty()) {
        try {
          publishedAt = LocalDateTime.parse(timestampStr,
              DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        } catch (Exception e) {
          log.warn("시간 파싱 실패: {}", timestampStr);
        }
      }

      String tagsJsonString = null;
      try {
        tagsJsonString = objectMapper.writeValueAsString(hashtags);
      } catch (Exception e) {
        log.error("해시태그 JSON 변환 실패: ", e);
        tagsJsonString = "[]";
      }

      // 썸네일 이미지 다운로드 및 저장
      File thumbnailFile = null;
      if (!displayUrl.isEmpty()) {
        thumbnailFile = imageService.downloadAndSaveThumbnailImage(displayUrl, platformAccount.getExternalAccountId(), externalContentId);
      }

      log.info("새 콘텐츠 생성: {}", externalContentId);
      return contentRepository.save(Content.builder()
          .platformAccountId(platformAccount.getId())
          .platformTypeId("INSTAGRAM")
          .contentTypeId(contentTypeId)
          .externalContentId(externalContentId)
          .title(title)
          .description(cleanDescription)
          .durationSeconds(durationSeconds)
          .thumbnailId(thumbnailFile != null ? thumbnailFile.getId() : null)
          .contentUrl(url)
          .publishedAt(publishedAt)
          .tagsJson(tagsJsonString)
          .totalViews(viewsCount)
          .totalLikes(likesCount)
          .totalComments(commentsCount)
          .snapshotDate(LocalDate.now())
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build());
    }
  }

  private void saveContentToS3(JsonNode contentNode, PlatformAccount platformAccount, Content savedContent) {
    try {
      // 크롤링 데이터에서 해시태그 파싱 (S3에는 원본 해시태그 저장)
      JsonNode hashtagsNode = contentNode.path("hashtags");
      List<String> hashtags = new ArrayList<>();
      if (hashtagsNode.isArray()) {
        for (JsonNode hashtag : hashtagsNode) {
          hashtags.add(hashtag.asText());
        }
      }

      ContentRawData.ThumbnailInfo thumbnailInfo = null;
      String displayUrl = contentNode.path("displayUrl").asText();
      if (!displayUrl.isEmpty()) {
        thumbnailInfo = ContentRawData.ThumbnailInfo.builder()
            .accessKey("raw_data/instagram/content_thumbnail_images/" + savedContent.getExternalContentId() + "/thumbnail.jpg")
            .contentType("image/jpeg")
            .build();
      }

      // DB에서 저장된 데이터와 크롤링 원본 데이터 조합
      ContentRawData rawData = ContentRawData.builder()
          .accountNickname(platformAccount.getAccountNickname())
          .platformType("INSTAGRAM")
          .externalContentId(savedContent.getExternalContentId())
          .contentType(savedContent.getContentTypeId())
          .title(savedContent.getTitle())
          .description(savedContent.getDescription())
          .durationSeconds(savedContent.getDurationSeconds())
          .contentUrl(savedContent.getContentUrl())
          .publishedAt(contentNode.path("timestamp").asText())  // 크롤링 원본 포맷 유지
          .tags(hashtags)  // 크롤링 원본 해시태그
          .viewsCount(savedContent.getTotalViews())  // DB 저장된 최신 값
          .likesCount(savedContent.getTotalLikes())
          .commentsCount(savedContent.getTotalComments())
          .thumbnailInfo(thumbnailInfo)
          .build();

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(rawData);

      LocalDate now = LocalDate.now();
      String s3Key = String.format("raw_data/instagram/content/%d/%02d/%02d/%s_%d.json",
          now.getYear(),
          now.getMonthValue(),
          now.getDayOfMonth(),
          savedContent.getExternalContentId(),
          System.currentTimeMillis());

      s3Service.uploadContentData(jsonData, s3Key);

    } catch (Exception e) {
      log.error("콘텐츠 S3 저장 중 오류: ", e);
    }
  }

  public void collectAllRegisteredUsersContent() {
    try {
      List<PlatformAccount> instagramAccounts = platformAccountRepository.findByPlatformTypeId("INSTAGRAM");

      log.info("등록된 Instagram 계정 {}개의 콘텐츠 수집 시작", instagramAccounts.size());

      for (PlatformAccount account : instagramAccounts) {
        try {
          log.info("계정 {} 콘텐츠 수집 시작", account.getAccountNickname());
          CompletableFuture<String> future = getContentsByUsername(account.getAccountNickname());
          future.get();

          Thread.sleep(5000);

        } catch (Exception e) {
          log.error("계정 {} 콘텐츠 수집 실패: ", account.getAccountNickname(), e);
        }
      }

      log.info("전체 계정 콘텐츠 수집 완료");

    } catch (Exception e) {
      log.error("전체 콘텐츠 수집 중 오류: ", e);
    }
  }

  // 해시태그 제거 메서드
  private String removeHashtagsFromText(String text, List<String> hashtags) {
    String result = text;
    for (String hashtag : hashtags) {
      result = result.replaceAll("#" + hashtag + "\\b", "").trim();
    }
    return result.replaceAll("\\s+", " ").trim(); // 다중 공백 정리
  }

  // 제목 추출 메서드
  private String extractTitle(String description) {
    if (description == null || description.isEmpty()) {
      return "Instagram 게시물";
    }

    // 첫 번째 줄만 사용
    String firstLine = description.split("\\n")[0].trim();

    // 50자 제한
    if (firstLine.length() > 50) {
      return firstLine.substring(0, 50) + "...";
    }

    return firstLine.isEmpty() ? "Instagram 게시물" : firstLine;
  }

//  /**
//   * 모든 콘텐츠 ID 조회
//   */
//  public List<Integer> getAllContentIds() {
//    return contentRepository.findAll().stream()
//        .map(Content::getId)
//        .collect(Collectors.toList());
//  }
  /**
   * Instagram 특정 계정의 콘텐츠 ID만 조회
   */
  public List<Integer> getInstagramPlaformAccountContentIds(Integer platformAccountId) {
    return contentRepository.findByPlatformTypeId("INSTAGRAM").stream()
        .filter(content -> content.getPlatformAccountId() >= platformAccountId)
        .map(Content::getId)
        .collect(Collectors.toList());
  }
  /**
   * Instagram 콘텐츠 ID만 조회
   */
  public List<Integer> getInstagramContentIds() {
    return contentRepository.findByPlatformTypeId("INSTAGRAM").stream()
//        .filter(content -> content.getId() >= 239)
        .map(Content::getId)
        .collect(Collectors.toList());
  }

}