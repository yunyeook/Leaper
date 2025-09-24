package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.crawling.instagram.dto.CommentRawData;
import com.ssafy.spark.domain.crawling.instagram.entity.Content;
import com.ssafy.spark.domain.crawling.instagram.repository.ContentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommentService extends BaseApifyService {

  private final ContentRepository contentRepository;
  private final S3Service s3Service;

  /**
   * 특정 콘텐츠의 댓글 수집
   */
  public CompletableFuture<String> getCommentsByContentId(Integer contentId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("댓글 수집 시작 - Content ID: {}", contentId);

        // Content 조회
        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 콘텐츠입니다: " + contentId));

        log.info("콘텐츠 확인됨 - URL: {}", content.getContentUrl());

        String actorId = "apify~instagram-comment-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("directUrls", new String[]{content.getContentUrl()});
        input.put("includeNestedComments", false);
        input.put("isNewestComments", false);
        input.put("resultsLimit", 10);// TODO : 댓글 몇개 수집할건지

        log.info("댓글 크롤링 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("댓글 수집 실행 - Run ID: {}", runId);

        return waitAndGetCommentResults(runId, content);

      } catch (Exception e) {
        log.error("댓글 수집 중 오류: ", e);
        return "{\"error\": \"댓글 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  private String waitAndGetCommentResults(String runId, Content content) {
    try {
      log.info("댓글 결과 대기 시작 - Run ID: {}", runId);

      boolean isCompleted = false;
      int maxAttempts = 30;
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("댓글 실행 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("댓글 수집 실패: {}", status);
          return "{\"error\": \"댓글 수집 실패: " + status + "\"}";
        }

        Thread.sleep(10000);
      }

      if (isCompleted) {
        String jsonResults = getRunResults(runId);
        log.info("댓글 수집 완료 - 데이터 크기: {} bytes", jsonResults.length());

        saveCommentsToS3(jsonResults, content);

        return jsonResults;
      } else {
        String errorMessage = String.format("댓글 수집 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        return "{\"error\": \"" + errorMessage + "\"}";
      }

    } catch (Exception e) {
      log.error("댓글 결과 대기 중 오류: ", e);
      return "{\"error\": \"댓글 결과 대기 중 오류: " + e.getMessage() + "\"}";
    }
  }

  private void saveCommentsToS3(String jsonResults, Content content) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);

      // 에러 응답 체크
      if (results.isArray() && results.size() > 0) {
        JsonNode firstItem = results.get(0);
        if (firstItem.has("error") || firstItem.has("requestErrorMessages")) {
          log.warn("댓글 수집 실패 - Content ID: {}, Error: {}",
              content.getId(), firstItem.path("errorDescription").asText());
          return;
        }
      }

      if (results.size() == 0) {
        log.warn("수집된 댓글이 없습니다.");
        return;
      }

      List<CommentRawData.CommentItem> commentItems = new ArrayList<>();
      String crawledAtTime = Instant.now().toString();

      for (JsonNode commentNode : results) {
        if (commentNode.has("error") || commentNode.has("requestErrorMessages")) {
          continue;
        }

        String accountNickname = commentNode.path("ownerUsername").asText();
        String externalCommentId = commentNode.path("id").asText();
        String text = commentNode.path("text").asText();
        Integer likesCount = commentNode.path("likesCount").asInt(0);
        String publishedAt = commentNode.path("timestamp").asText();

        if (accountNickname.isEmpty() || externalCommentId.isEmpty()) {
          continue;
        }

        CommentRawData.CommentItem commentItem = CommentRawData.CommentItem.builder()
            .accountNickname(accountNickname)
            .externalCommentId(externalCommentId)
            .text(text)
            .likesCount(likesCount)
            .publishedAt(publishedAt)
            .build();

        commentItems.add(commentItem);
      }

      if (commentItems.isEmpty()) {
        log.info("유효한 댓글이 없어 S3에 저장하지 않습니다 - Content ID: {}", content.getId());
        return;
      }

      CommentRawData commentRawData = CommentRawData.builder()
          .contentId(content.getId())
          .platformAccountId(content.getPlatformAccountId())
          .contentUrl(content.getContentUrl())
          .commentsCount(commentItems.size())
          .comments(commentItems)
          .crawledAt(crawledAtTime)
          .build();

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(commentRawData);
      LocalDate now = LocalDate.now();
      String s3Key = String.format("raw_data/instagram/comment/%d/%02d/%02d/content_%d_%d.json",
          now.getYear(),
          now.getMonthValue(),
          now.getDayOfMonth(),
          content.getId(),
          System.currentTimeMillis());

      s3Service.uploadContentData(jsonData, s3Key);

      log.info("댓글 S3 저장 완료 - Content ID: {}, 댓글 수: {}", content.getId(), commentItems.size());

    } catch (Exception e) {
      log.error("댓글 S3 저장 중 오류: ", e);
    }
  }  /**
   * 모든 콘텐츠의 댓글 수집
   */
  public void collectAllContentComments() {
    try {
      List<Content> contents = contentRepository.findAll();
      log.info("전체 콘텐츠 {}개의 댓글 수집 시작", contents.size());

      for (Content content : contents) {
        try {
          log.info("콘텐츠 {} 댓글 수집 시작", content.getId());
          CompletableFuture<String> future = getCommentsByContentId(content.getId());
          future.get();

          Thread.sleep(5000); // Rate limiting 방지

        } catch (Exception e) {
          log.error("콘텐츠 {} 댓글 수집 실패: ", content.getId(), e);
        }
      }

      log.info("전체 콘텐츠 댓글 수집 완료");

    } catch (Exception e) {
      log.error("전체 댓글 수집 중 오류: ", e);
    }
  }

  /**
   * 배치 댓글 수집 (기존 개별 API 활용)
   */
  public void collectCommentsBatchUsingExistingApi(List<Integer> contentIds, int batchSize) {
    try {
      log.info("기존 API 활용 배치 댓글 수집 시작 - 총 {}개 콘텐츠, 배치 크기: {}", contentIds.size(), batchSize);

      // 배치 단위로 나누어서 처리
      for (int i = 0; i < contentIds.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, contentIds.size());
        List<Integer> batchIds = contentIds.subList(i, endIndex);

        log.info("배치 {}/{} 처리 중 ({}~{}번째)",
            (i/batchSize) + 1,
            (contentIds.size() + batchSize - 1) / batchSize,
            i + 1, endIndex);

        // 배치 내 개별 처리
        for (Integer contentId : batchIds) {
          try {
            log.info("개별 댓글 수집 시작 - Content ID: {}", contentId);

            // 🟢 기존에 잘 되는 개별 메서드 사용
            CompletableFuture<String> future = getCommentsByContentId(contentId);
            String result = future.get();

            log.info("개별 댓글 수집 완료 - Content ID: {}", contentId);

            // 개별 처리 간 대기 (API 제한 방지)
            Thread.sleep(3000); // 3초 대기

          } catch (Exception e) {
            log.error("개별 댓글 수집 실패 - Content ID: {}", contentId, e);
          }
        }

        // 배치 간 대기 (API 제한 방지)
        if (endIndex < contentIds.size()) {
          Thread.sleep(10000); // 10초 대기
          log.info("다음 배치까지 10초 대기...");
        }
      }

      log.info("기존 API 활용 배치 댓글 수집 완료");

    } catch (Exception e) {
      log.error("배치 댓글 수집 중 오류: ", e);
      throw new RuntimeException("배치 댓글 수집 실패", e);
    }
  }

  /**
   * 여러 콘텐츠 ID들에 대한 댓글 수집
   */
  public CompletableFuture<String> getCommentsByContentIds(String accountNickname, List<String> contentIds) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("배치 댓글 수집 시작: {} ({}개 콘텐츠)", accountNickname, contentIds.size());

        int successCount = 0;
        int failCount = 0;

        for (String contentIdStr : contentIds) {
          try {
            Integer contentId = Integer.parseInt(contentIdStr);

            // 기존 메서드 활용
            CompletableFuture<String> future = getCommentsByContentId(contentId);
            String result = future.get();

            successCount++;
            log.info("댓글 수집 완료 - Content ID: {} ({}/{})",
                contentId, successCount + failCount, contentIds.size());

            // API 제한 방지를 위한 대기
            Thread.sleep(3000);

          } catch (Exception e) {
            failCount++;
            log.error("댓글 수집 실패 - Content ID: {}", contentIdStr, e);
          }
        }

        String result = String.format("배치 댓글 수집 완료 - 성공: %d, 실패: %d", successCount, failCount);
        log.info(result);
        return result;

      } catch (Exception e) {
        log.error("배치 댓글 수집 중 오류: ", e);
        return "댓글 수집 실패: " + e.getMessage();
      }
    });
  }
}