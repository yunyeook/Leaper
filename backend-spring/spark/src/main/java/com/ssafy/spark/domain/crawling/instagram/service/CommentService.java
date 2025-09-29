package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.repository.ContentRepository;
import com.ssafy.spark.domain.crawling.instagram.dto.CommentRawData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CommentService extends BaseApifyService {

  private final ContentRepository contentRepository;
  private final CrawlingDataSaveToS3Service s3Service;
  private final String platformType = "instagram";

  /**
   * 특정 콘텐츠(1개)의 댓글 수집
   */
  public CompletableFuture<String> getCommentsByContentId(Integer contentId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new RuntimeException("존재하지 않는 콘텐츠입니다: " + contentId));

        String actorId = "apify~instagram-comment-scraper";
        Map<String, Object> input = new HashMap<>();
        input.put("directUrls", new String[]{content.getContentUrl()});
        input.put("includeNestedComments", false);
        input.put("isNewestComments", false);
        input.put("resultsLimit", 10);// TODO : 댓글 몇개 수집할건지

        String runId = runActor(actorId, input);

        return waitAndGetCommentResults(runId, content);

      } catch (Exception e) {
        return "{\"error\": \"댓글 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  private String waitAndGetCommentResults(String runId, Content content) {
    try {
      boolean isCompleted = false;
      int maxAttempts = 30; // apify에게 크롤링 완료되었는지 몇번이나 확인할건지 (기존 api호출이랑은 다른거임)
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          return "{\"error\": \"댓글 수집 실패: " + status + "\"}";
        }

        Thread.sleep(5000); // 5초마다 크롤링 완료되었는지 확인

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
        Integer likesCount = Math.max(0, commentNode.path("likesCount").asInt(0));
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
          .platformAccountId(content.getPlatformAccount().getId())
          .contentUrl(content.getContentUrl())
          .commentsCount(commentItems.size())
          .comments(commentItems)
          .crawledAt(crawledAtTime)
          .build();

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(commentRawData);

      // JSON + Parquet 저장
      var paths = s3Service.uploadCommentData(jsonData, platformType, content.getId().toString());
      
      log.info("댓글 저장 완료 - Content ID: {}, 댓글 수: {}", content.getId(), commentItems.size());
      log.debug("  - JSON: {}", paths.get("json"));
      log.debug("  - Parquet: {}", paths.get("parquet"));

    } catch (Exception e) {
      log.error("댓글 S3 저장 중 오류: ", e);
    }
  }
  /**
   * DB에 저장된 모든 인스타그램 콘텐츠의 댓글 수집 : 스케줄러로 실행할것
   */
  public void collectAllContentComments() {
    try {
      // 댓글 수집할 인스타그램 콘텐츠 조회
      List<Content> contents = contentRepository.findByPlatformTypeId("INSTAGRAM");
      log.info("인스타그램 콘텐츠 {}개의 댓글 수집 시작", contents.size());

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

      log.info("인스타그램 콘텐츠 댓글 수집 완료");

    } catch (Exception e) {
      log.error("인스타그램 댓글 수집 중 오류: ", e);
    }
  }


  public CompletableFuture<String> getCommentsByContents(String username, List<Content> contents) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // 각 콘텐츠별 비동기 작업 실행
        List<CompletableFuture<String>> futures = contents.stream()
            .map(content -> {
              log.info("콘텐츠 {} 댓글 수집 시작", content.getId());
              return getCommentsByContentId(content.getId());
            })
            .collect(Collectors.toList());

        // 모든 작업 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("계정 {} - 인스타그램 댓글 수집 완료", username);
        return "{\"status\": \"success\"}";
      } catch (Exception e) {
        log.error("인스타그램 댓글 수집 중 오류: ", e);
        return "{\"error\": \"댓글 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }


}