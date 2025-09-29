package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.repository.ContentRepository;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.business.type.entity.ContentType;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.domain.business.type.service.ContentTypeService;
import com.ssafy.spark.domain.business.type.service.PlatformTypeService;
import com.ssafy.spark.domain.crawling.instagram.dto.ContentRawData;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InstagramContentService extends BaseApifyService {

  private final ContentRepository contentRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final CrawlingDataSaveToS3Service s3Service;
  private final ImageService imageService;
  private final ContentTypeService contentTypeService;
  private final PlatformTypeService platformTypeService;
  private final String platformType = "instagram";

  /**
   * 모든 계정의 컨텐츠 크롤링
   */
  public void allContentsCrawling() {
    try {
      List<PlatformAccount> accounts = platformAccountRepository.findAllByPlatformTypeId("INSTAGRAM");
      log.info("등록된 Instagram 계정 수: {}", accounts.size());

      LocalDate today = LocalDate.now();

      for (PlatformAccount account : accounts) {
        try {
          boolean alreadyCollected = contentRepository.existsByPlatformAccountAndSnapshotDate(account, today);
          if (alreadyCollected) {
            log.info("계정 {} - 오늘 이미 수집됨, 건너뜀", account.getAccountNickname());
            continue;
          }

          log.info("콘텐츠 수집 시작 - 계정: {}", account.getAccountNickname());
          String runId = executeContentScraper(account.getAccountNickname());
          String result = waitForRunCompletion(runId);
          saveContents(result, account);

          Thread.sleep(5000); // rate limit 대응

        } catch (Exception e) {
          log.error("콘텐츠 수집 실패 - 계정: {}", account.getAccountNickname(), e);
        }
      }

    } catch (Exception e) {
      log.error("전체 콘텐츠 수집 중 오류 발생", e);
    }
  }

  /**
   * 단일 계정의 콘텐츠 수집
   */

  public CompletableFuture<String> contentCrawling(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        PlatformAccount account = platformAccountRepository.findByAccountNickname(username)
            .orElseThrow(() -> new RuntimeException("등록되지 않은 사용자입니다: " + username));

        String runId = executeContentScraper(username);
        String result = waitForRunCompletion(runId);
        saveContents(result, account);

        return result;
      } catch (Exception e) {
        log.error("콘텐츠 수집 실패 - 사용자: {}", username, e);
        return "{\"error\":\"콘텐츠 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }




  /**
   * 컨텐츠 저장
   */
  private void saveContents(String jsonResults, PlatformAccount account) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      if (results.isEmpty()) {
        log.warn("콘텐츠 결과가 비어 있음 - 계정: {}", account.getAccountNickname());
        return;
      }

      for (JsonNode contentNode : results) {
        try {
          Content saved = saveContentToDatabase(contentNode, account);
          saveContentToS3(contentNode, account, saved);
        } catch (Exception e) {
          log.error("콘텐츠 저장 실패 - externalId: {}", contentNode.path("id").asText(), e);
        }
      }

    } catch (Exception e) {
      log.error("콘텐츠 파싱 중 오류", e);
    }
  }

  /**
   * DB 저장 (기존 업데이트 or 신규 삽입)
   */
  private Content saveContentToDatabase(JsonNode node, PlatformAccount account) {
    String externalContentId = node.path("id").asText();
    Optional<Content> existing = contentRepository.findByExternalContentId(externalContentId);

    Long likes = Math.max(0, node.path("likesCount").asLong(0));
    Long comments = Math.max(0, node.path("commentsCount").asLong(0));
    Long views = Math.max(0, Math.max(node.path("videoViewCount").asLong(0), node.path("videoPlayCount").asLong(0)));

    if (existing.isPresent()) {
      Content content = existing.get();
      content.setTotalLikes(BigInteger.valueOf(likes));
      content.setTotalComments(BigInteger.valueOf(comments));
      content.setTotalViews(BigInteger.valueOf(views));
      content.setSnapshotDate(LocalDate.now());
      content.setUpdatedAt(LocalDateTime.now());
      return contentRepository.save(content);
    }

    String type = node.path("type").asText();
    String contentTypeId = "Sidecar".equals(type) ? "POST" : "VIDEO_SHORT";
    String caption = node.path("caption").asText();
    String url = node.path("url").asText();
    String displayUrl = node.path("displayUrl").asText();
    String timestampStr = node.path("timestamp").asText();

    List<String> hashtags = new ArrayList<>();
    if (node.has("hashtags") && node.path("hashtags").isArray()) {
      node.path("hashtags").forEach(tag -> hashtags.add(tag.asText()));
    }

    String cleanDescription = removeHashtagsFromText(caption, hashtags);
    String title = extractTitle(cleanDescription);
    LocalDateTime publishedAt = parseTimestamp(timestampStr);

    File thumbnailFile = displayUrl.isEmpty()
        ? null
        : imageService.downloadAndSaveThumbnailImage(displayUrl, account.getExternalAccountId(), externalContentId);

    PlatformType platformEntity = platformTypeService.findEntityByPlatformTypeId("INSTAGRAM")
        .orElseThrow(() -> new IllegalArgumentException("INSTAGRAM 플랫폼 타입을 찾을 수 없습니다"));
    ContentType contentEntity = contentTypeService.findEntityByContentTypeId(contentTypeId)
        .orElseThrow(() -> new IllegalArgumentException("콘텐츠 타입을 찾을 수 없습니다: " + contentTypeId));

    return contentRepository.save(Content.builder()
        .platformAccount(account)
        .platformType(platformEntity)
        .contentType(contentEntity)
        .externalContentId(externalContentId)
        .title(title)
        .description(cleanDescription)
        .thumbnailId(thumbnailFile != null ? thumbnailFile.getId() : null)
        .contentUrl(url)
        .publishedAt(publishedAt)
        .tagsJson(hashtags)
        .totalViews(BigInteger.valueOf(views))
        .totalLikes(BigInteger.valueOf(likes))
        .totalComments(BigInteger.valueOf(comments))
        .snapshotDate(LocalDate.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build());
  }

  /**
   *  S3 저장
   */
  private void saveContentToS3(JsonNode node, PlatformAccount account, Content saved) {
    try {
      List<String> hashtags = new ArrayList<>();
      if (node.has("hashtags") && node.path("hashtags").isArray()) {
        node.path("hashtags").forEach(tag -> hashtags.add(tag.asText()));
      }

      ContentRawData.ThumbnailInfo thumbnailInfo = null;
      String displayUrl = node.path("displayUrl").asText();
      if (!displayUrl.isEmpty()) {
        thumbnailInfo = ContentRawData.ThumbnailInfo.builder()
            .accessKey("raw_data/instagram/content_thumbnail_images/" + saved.getExternalContentId() + "/thumbnail.jpg")
            .contentType("image/jpeg")
            .build();
      }

      ContentRawData raw = ContentRawData.builder()
          .accountNickname(account.getAccountNickname())
          .platformType("INSTAGRAM")
          .externalContentId(saved.getExternalContentId())
          .contentType(saved.getContentType().getId())
          .title(saved.getTitle())
          .description(saved.getDescription())
          .durationSeconds(saved.getDurationSeconds())
          .contentUrl(saved.getContentUrl())
          .publishedAt(node.path("timestamp").asText())
          .tags(hashtags)
          .viewsCount(saved.getTotalViews().longValue())
          .likesCount(saved.getTotalLikes().longValue())
          .commentsCount(saved.getTotalComments().longValue())
          .thumbnailInfo(thumbnailInfo)
          .build();

      String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(raw);
      
      // JSON + Parquet 저장
      var paths = s3Service.uploadContentData(json, platformType, saved.getExternalContentId());
      
      log.info("콘텐츠 저장 완료 - ID: {}", saved.getExternalContentId());
      log.debug("  - JSON: {}", paths.get("json"));
      log.debug("  - Parquet: {}", paths.get("parquet"));

    } catch (Exception e) {
      log.error("S3 저장 중 오류 - 콘텐츠 ID: {}", saved.getExternalContentId(), e);
    }
  }

  /**
   * ✅ 유틸 메서드
   */
  private String removeHashtagsFromText(String text, List<String> hashtags) {
    if (text == null) return "";
    String result = text;
    for (String tag : hashtags) {
      result = result.replaceAll("#" + tag, "");
    }
    return result.trim();
  }

  private String extractTitle(String text) {
    if (text == null || text.isBlank()) return "Untitled";
    return text.length() > 30 ? text.substring(0, 30) + "..." : text;
  }

  private LocalDateTime parseTimestamp(String timestamp) {
    try {
      return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    } catch (Exception e) {
      return null;
    }
  }
}
