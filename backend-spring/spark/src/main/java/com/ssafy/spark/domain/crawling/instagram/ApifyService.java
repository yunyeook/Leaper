package com.ssafy.spark.domain.crawling.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApifyService {

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  @Value("${apify.api-key}")
  private String apiKey;

  @Value("${apify.base-url}")
  private String baseUrl; // https://api.apify.com/v2

  /**
   * Apify API 연결 테스트
   */
  public String testApifyConnection() {
    try {
      log.info("Apify 연결 테스트 시작...");
      log.info("API 키: {}", apiKey != null ? "설정됨 (길이: " + apiKey.length() + ")" : "null");
      log.info("Base URL: {}", baseUrl);

      if (apiKey == null || apiKey.trim().isEmpty()) {
        return "API 키가 설정되지 않았습니다. application.yml을 확인하세요.";
      }

      String url = String.format("%s/users/me?token=%s", baseUrl, apiKey);
      log.info("테스트 URL: {}", url);

      String response = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.error("API 테스트 에러 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("API 테스트 실패: " + body);
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(10))
          .block();

      log.info("API 응답: {}", response);
      return "Apify API 연결 성공: " + response;

    } catch (Exception e) {
      log.error("Apify 연결 테스트 실패: ", e);
      return "Apify API 연결 실패: " + e.getMessage();
    }
  }

  /**
   * 1. 프로필 정보만 수집
   */
  public CompletableFuture<String> getProfileOnly(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("프로필 정보만 수집 시작: {}", username);

        String actorId = "apify~instagram-profile-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsType", "details");
        input.put("usernames", new String[]{username});
        input.put("resultsLimit", 1);

        log.info("프로필 전용 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("프로필 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "프로필 정보");

      } catch (Exception e) {
        log.error("프로필 수집 중 오류: ", e);
        return "{\"error\": \"프로필 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }


  /**
   * 2. 게시물 정보만 수집
   */
  public CompletableFuture<String> getPostsOnly(String username, int postsLimit) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("게시물 정보만 수집 시작: {} ({}개)", username, postsLimit);

        String actorId = "apify~instagram-post-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsLimit", postsLimit);
        input.put("skipPinnedPosts", false);
        input.put("username", new String[]{username}); // 단일 사용자명을 배열로

        log.info("게시물 전용 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("게시물 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "게시물 정보");

      } catch (Exception e) {
        log.error("게시물 수집 중 오류: ", e);
        return "{\"error\": \"게시물 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }


  /**
   * 3. 프로필 + 게시물 함께 수집
   */
  public CompletableFuture<String> getProfileAndPosts(String username, int postsLimit) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("프로필 + 게시물 함께 수집 시작: {} ({}개)", username, postsLimit);

        String actorId = "apify~instagram-api-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("addParentData", false);
        input.put("directUrls", new String[]{"https://www.instagram.com/" + username + "/"});
        input.put("enhanceUserSearchWithFacebookPage", false);
        input.put("isUserReelFeedURL", false);
        input.put("isUserTaggedFeedURL", false);
        input.put("resultsLimit", postsLimit);
        input.put("resultsType", "details"); // 프로필 + 게시물 모두

        log.info("프로필+게시물 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("프로필+게시물 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "프로필 + 게시물");

      } catch (Exception e) {
        log.error("프로필+게시물 수집 중 오류: ", e);
        return "{\"error\": \"프로필+게시물 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  /**
   * 4. 해시태그로 게시물 수집
   */
  public CompletableFuture<String> getHashtagPosts(String hashtag, int postsLimit) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("해시태그 게시물 수집 시작: #{} ({}개)", hashtag, postsLimit);

        String actorId = "apify~instagram-hashtag-scraper";

        // 해시태그 API 스펙에 맞는 입력
        Map<String, Object> input = new HashMap<>();
        input.put("hashtags", new String[]{hashtag});
        input.put("resultsLimit", postsLimit);
        input.put("resultsType", "posts");

        log.info("해시태그 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("해시태그 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "해시태그 게시물");

      } catch (Exception e) {
        log.error("해시태그 수집 중 오류: ", e);
        return "{\"error\": \"해시태그 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }


  /**
   * 5. 댓글 수집 (새로 추가)
   */
  public CompletableFuture<String> getComments(List<String> directUrls, int resultsLimit,
      boolean includeNestedComments, boolean isNewestComments) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("댓글 수집 시작: {} 개 URL, {}개 댓글", directUrls.size(), resultsLimit);
        log.info("수집 대상 URL: {}", String.join(", ", directUrls));

        String actorId = "apify~instagram-comment-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("directUrls", directUrls.toArray(new String[0]));
        input.put("resultsLimit", resultsLimit);
        input.put("includeNestedComments", includeNestedComments);
        input.put("isNewestComments", isNewestComments);

        log.info("댓글 수집 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("댓글 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "댓글 정보");

      } catch (Exception e) {
        log.error("댓글 수집 중 오류: ", e);
        return "{\"error\": \"댓글 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  /**
   * 6. 단일 게시물 댓글 수집 (편의 메서드)
   */
  public CompletableFuture<String> getPostComments(String postUrl, int resultsLimit) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("단일 게시물 댓글 수집 시작: {} ({}개)", postUrl, resultsLimit);

        // 유효한 Instagram URL인지 검증
        if (!postUrl.contains("instagram.com/") ||
            (!postUrl.contains("/p/") && !postUrl.contains("/reel/"))){
          return "{\"error\": \"유효한 Instagram 게시물 URL이 아닙니다. /p/ 또는 /reel/ 형태여야 합니다.\"}";
        }

        String actorId = "apify~instagram-comment-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("directUrls", new String[]{postUrl});
        input.put("resultsLimit", resultsLimit);
        input.put("includeNestedComments", false);
        input.put("isNewestComments", false);

        log.info("단일 게시물 댓글 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("단일 게시물 댓글 수집 실행 - Run ID: {}", runId);

        return waitAndGetResults(runId, "단일 게시물 댓글");

      } catch (Exception e) {
        log.error("단일 게시물 댓글 수집 중 오류: ", e);
        return "{\"error\": \"단일 게시물 댓글 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }
  /**
   * Apify Actor 실행
   */
  private String runActor(String actorId, Map<String, Object> input) {
    try {
      String url = String.format("%s/acts/%s/runs?token=%s", baseUrl, actorId, apiKey);

      log.info("Actor 실행 요청: {}", url);
      log.info("Input: {}", objectMapper.writeValueAsString(input));

      String response = webClient.post()
          .uri(url)
          .header("Content-Type", "application/json")
          .bodyValue(input)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.error("Actor 실행 실패 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("Actor 실행 실패: " + body);
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(30))
          .block();

      log.info("Actor 실행 응답: {}", response);

      JsonNode jsonNode = objectMapper.readTree(response);
      String runId = jsonNode.path("data").path("id").asText();

      if (runId.isEmpty()) {
        log.error("Run ID를 찾을 수 없습니다. 응답: {}", response);
        throw new RuntimeException("Run ID 추출 실패");
      }

      return runId;

    } catch (Exception e) {
      log.error("Actor 실행 중 오류: ", e);
      throw new RuntimeException("Actor 실행 실패", e);
    }
  }

  /**
   * Actor 실행 완료 대기 및 결과 반환 (JSON 데이터 반환)
   */
  private String waitAndGetResults(String runId, String dataType) {
    try {
      log.info("결과 대기 시작 - Run ID: {}", runId);

      // 실행 완료 대기 (최대 5분)
      boolean isCompleted = false;
      int maxAttempts = 30; // 30번 시도 (각 10초씩)
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("실행 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("Actor 실행 실패: {}", status);
          return "{\"error\": \"실행 실패: " + status + "\"}";
        }

        Thread.sleep(10000); // 10초 대기
      }

      if (isCompleted) {
        // 결과 데이터 가져오기
        String jsonResults = getRunResults(runId);
        log.info("{} 수집 완료 - 데이터 크기: {} bytes", dataType, jsonResults.length());

        // 로그에도 결과 출력 (선택사항)
        printResults(jsonResults, dataType);

        // JSON 데이터 그대로 반환
        return jsonResults;
      } else {
        String errorMessage = String.format("실행 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        return "{\"error\": \"" + errorMessage + "\"}";
      }

    } catch (Exception e) {
      log.error("결과 대기 중 오류: ", e);
      return "{\"error\": \"결과 대기 중 오류: " + e.getMessage() + "\"}";
    }
  }

  /**
   * Actor 실행 상태 확인
   */
  private String checkRunStatus(String runId) {
    try {
      String url = String.format("%s/actor-runs/%s?token=%s", baseUrl, runId, apiKey);

      String response = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.warn("상태 확인 실패 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("상태 확인 실패");
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(10))
          .block();

      JsonNode jsonNode = objectMapper.readTree(response);
      return jsonNode.path("data").path("status").asText();

    } catch (Exception e) {
      log.error("상태 확인 중 오류: ", e);
      return "ERROR";
    }
  }

  /**
   * Actor 실행 결과 가져오기
   */
  private String getRunResults(String runId) {
    try {
      String url = String.format("%s/actor-runs/%s/dataset/items?token=%s", baseUrl, runId, apiKey);
      log.info("결과 가져오기 URL: {}", url);

      String response = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.warn("결과 가져오기 실패 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("결과 가져오기 실패");
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(30))
          .block();

      log.info("결과 데이터 크기: {} bytes", response != null ? response.length() : 0);
      return response != null ? response : "[]";

    } catch (Exception e) {
      log.error("결과 가져오기 중 오류: ", e);
      return "[]";
    }
  }

  /**
   * 결과 데이터 콘솔 출력 - 개선된 버전
   */
  private void printResults(String jsonResults, String dataType) {
    try {
      if (jsonResults == null || jsonResults.trim().isEmpty() || "[]".equals(jsonResults.trim())) {
        log.warn("수집된 {}이(가) 없습니다.", dataType);
        return;
      }

      JsonNode results = objectMapper.readTree(jsonResults);

      log.info("=== {} 수집 결과 ===", dataType);
      log.info("총 {}개 항목 수집됨", results.size());

      if (results.size() == 0) {
        log.warn("결과가 비어있습니다. 사용자명이 올바른지 또는 계정이 공개되어 있는지 확인하세요.");
        return;
      }

      for (int i = 0; i < Math.min(results.size(), 3); i++) { // 최대 3개만 출력
        JsonNode item = results.get(i);
        log.info("--- 항목 {} ---", i + 1);

        // 프로필 정보 출력
        printIfExists(item, "username", "사용자명");
        printIfExists(item, "fullName", "전체 이름");
        printIfExists(item, "biography", "소개");
        printIfExists(item, "followersCount", "팔로워 수");
        printIfExists(item, "followingCount", "팔로잉 수");
        printIfExists(item, "postsCount", "게시물 수");

        // 게시물 정보 출력
        printIfExists(item, "caption", "게시물 캡션");
        printIfExists(item, "likesCount", "좋아요 수");
        printIfExists(item, "timestamp", "게시일");
        printIfExists(item, "url", "URL");

        // 전체 JSON 출력 (디버깅용)
        log.debug("전체 데이터 {}: {}", i + 1, objectMapper.writeValueAsString(item));
      }

      if (results.size() > 3) {
        log.info("... 외 {}개 항목 더 있음 (로그 레벨을 DEBUG로 설정하면 전체 데이터를 볼 수 있습니다)", results.size() - 3);
      }

    } catch (Exception e) {
      log.error("결과 출력 중 오류: ", e);
      log.info("Raw 결과 (처음 500자): {}",
          jsonResults != null && jsonResults.length() > 500 ?
              jsonResults.substring(0, 500) + "..." : jsonResults);
    }
  }

  /**
   * JSON 필드가 존재하면 출력하는 헬퍼 메서드
   */
  private void printIfExists(JsonNode item, String fieldName, String displayName) {
    if (item.has(fieldName) && !item.path(fieldName).isNull()) {
      JsonNode field = item.path(fieldName);
      if (field.isTextual()) {
        String text = field.asText();
        if (!text.isEmpty()) {
          log.info("{}: {}", displayName, text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }
      } else {
        log.info("{}: {}", displayName, field.asText());
      }
    }
  }


}