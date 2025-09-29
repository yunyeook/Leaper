package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.spark.global.config.ApifyApiConfig;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseApifyService {

  @Autowired
  protected WebClient webClient;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  private ApifyApiConfig apifyApiConfig;

  @Value("${apify.base-url}")
  protected String baseUrl; // https://api.apify.com/v2

  private static final int MAX_RETRY_COUNT = 12;
  /**
   * ✅ Apify 실행 공통 메서드
   */
  protected String executeProfileScraper(String username) {
    try {
      String actorId = "apify~instagram-profile-scraper";
      Map<String, Object> input = new HashMap<>();
      input.put("resultsType", "details");
      input.put("usernames", new String[]{username});
      input.put("resultsLimit", 1);
      return runActor(actorId, input);
    } catch (Exception e) {
      log.error("Apify 실행 중 오류 - 사용자: {}", username, e);
      throw new RuntimeException("Apify 실행 실패", e);
    }
  }
  /**
   * ✅ Apify 콘텐츠 크롤러 실행
   */
  protected String executeContentScraper(String username) {
    try {
      String actorId = "apify~instagram-post-scraper";
      Map<String, Object> input = new HashMap<>();
      input.put("resultsLimit", 10);
      input.put("skipPinnedPosts", false);
      input.put("username", new String[]{username});

      log.info("콘텐츠 수집 요청 - 사용자: {}", username);
      return runActor(actorId, input);
    } catch (Exception e) {
      log.error("Apify 실행 중 오류 - 사용자: {}", username, e);
      throw new RuntimeException("Apify 실행 실패", e);
    }
  }
  /**
   * Apify 실행 결과 대기 및 결과 반환
   */
  protected String waitForRunCompletion(String runId) {
    try {
      int maxAttempts = 30;

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        if ("SUCCEEDED".equals(status)) {
          return getRunResults(runId);
        }
        if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          throw new RuntimeException("크롤링 실패 상태: " + status);
        }
        Thread.sleep(5000);
      }
      throw new RuntimeException("크롤링 시간 초과");
    } catch (Exception e) {
      log.error("크롤링 대기 중 오류 - Run ID: {}", runId, e);
      throw new RuntimeException("크롤링 실패", e);
    }
  }
  /**
   * Apify Actor 실행 (12번 재시도 포함)
   */
  protected String runActor(String actorId, Map<String, Object> input) {
    return runActorWithRetry(actorId, input, 0);
  }

  private String runActorWithRetry(String actorId, Map<String, Object> input, int attempt) {
    if (attempt >= MAX_RETRY_COUNT) {
      throw new RuntimeException("모든 Apify API 키에서 실패했습니다 (" + MAX_RETRY_COUNT + "번 시도)");
    }

    try {
      String currentApiKey = apifyApiConfig.getCurrentKey();
      String url = String.format("%s/acts/%s/runs?token=%s", baseUrl, actorId, currentApiKey);

      log.info("Actor 실행 시도 {}/{} - 키 인덱스: {}",
          attempt + 1, MAX_RETRY_COUNT, apifyApiConfig.getCurrentKeyIndex() + 1);

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
                      return new RuntimeException("HTTP " + clientResponse.statusCode() + ": " + body);
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(30))
          .block();

      JsonNode jsonNode = objectMapper.readTree(response);
      String runId = jsonNode.path("data").path("id").asText();

      if (runId.isEmpty()) {
        throw new RuntimeException("Run ID 추출 실패");
      }

      log.info("Actor 실행 성공 - Run ID: {}", runId);
      return runId;

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      boolean isQuotaError = isQuotaOrRateLimitError(errorMessage);

      if (isQuotaError && attempt < MAX_RETRY_COUNT - 1) {
        log.warn("Apify 할당량/제한 감지 ({}/{}), 다음 키로 전환: {}",
            attempt + 1, MAX_RETRY_COUNT, errorMessage);
        apifyApiConfig.switchToNextKey();
        return runActorWithRetry(actorId, input, attempt + 1);
      }

      log.error("Apify Actor 실행 최종 실패 - 시도: {}/{}, 오류: {}",
          attempt + 1, MAX_RETRY_COUNT, errorMessage);
      throw new RuntimeException("Actor 실행 실패 (모든 키 시도 완료)", e);
    }
  }

  /**
   * Actor 실행 상태 확인
   */
  protected String checkRunStatus(String runId) {
    return checkRunStatusWithRetry(runId, 0);
  }

  private String checkRunStatusWithRetry(String runId, int attempt) {
    if (attempt >= MAX_RETRY_COUNT) {
      log.error("상태 확인 모든 키에서 실패: {}", runId);
      return "ERROR";
    }

    try {
      String currentApiKey = apifyApiConfig.getCurrentKey();
      String url = String.format("%s/actor-runs/%s?token=%s", baseUrl, runId, currentApiKey);

      String response = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.warn("상태 확인 실패 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("HTTP " + clientResponse.statusCode() + ": " + body);
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(10))
          .block();

      JsonNode jsonNode = objectMapper.readTree(response);
      return jsonNode.path("data").path("status").asText();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      boolean isQuotaError = isQuotaOrRateLimitError(errorMessage);

      if (isQuotaError && attempt < MAX_RETRY_COUNT - 1) {
        log.warn("상태 확인 할당량/제한 감지 ({}/{}), 다음 키로 전환",
            attempt + 1, MAX_RETRY_COUNT);
        apifyApiConfig.switchToNextKey();
        return checkRunStatusWithRetry(runId, attempt + 1);
      }

      log.error("상태 확인 중 오류 (시도: {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, errorMessage);
      return "ERROR";
    }
  }

  /**
   * Actor 실행 결과 가져오기
   */
  protected String getRunResults(String runId) {
    return getRunResultsWithRetry(runId, 0);
  }

  private String getRunResultsWithRetry(String runId, int attempt) {
    if (attempt >= MAX_RETRY_COUNT) {
      log.error("결과 가져오기 모든 키에서 실패: {}", runId);
      return "[]";
    }

    try {
      String currentApiKey = apifyApiConfig.getCurrentKey();
      String url = String.format("%s/actor-runs/%s/dataset/items?token=%s", baseUrl, runId, currentApiKey);

      log.info("결과 가져오기 시도 {}/{} - Run ID: {}",
          attempt + 1, MAX_RETRY_COUNT, runId);

      String response = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(
              status -> status.is4xxClientError() || status.is5xxServerError(),
              clientResponse -> {
                return clientResponse.bodyToMono(String.class)
                    .map(body -> {
                      log.warn("결과 가져오기 실패 - 상태: {}, 응답: {}", clientResponse.statusCode(), body);
                      return new RuntimeException("HTTP " + clientResponse.statusCode() + ": " + body);
                    });
              })
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(30))
          .block();

      log.info("결과 데이터 크기: {} bytes", response != null ? response.length() : 0);
      return response != null ? response : "[]";

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      boolean isQuotaError = isQuotaOrRateLimitError(errorMessage);

      if (isQuotaError && attempt < MAX_RETRY_COUNT - 1) {
        log.warn("결과 가져오기 할당량/제한 감지 ({}/{}), 다음 키로 전환: {}",
            attempt + 1, MAX_RETRY_COUNT, errorMessage);
        apifyApiConfig.switchToNextKey();
        return getRunResultsWithRetry(runId, attempt + 1);
      }

      log.error("결과 가져오기 중 오류 (시도: {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, errorMessage);
      return "[]";
    }
  }

  /**
   * 할당량 또는 Rate Limit 에러 감지
   */
  private boolean isQuotaOrRateLimitError(String errorMessage) {
    if (errorMessage == null) {
      return false;
    }

    String lowerMessage = errorMessage.toLowerCase();

    return lowerMessage.contains("http 402") ||          // Payment Required
        lowerMessage.contains("http 403") ||          // Forbidden
        lowerMessage.contains("http 429") ||          // Too Many Requests
        lowerMessage.contains("402") ||
        lowerMessage.contains("403") ||
        lowerMessage.contains("429") ||
        lowerMessage.contains("insufficient_credits") ||
        lowerMessage.contains("rate_limit_exceeded") ||
        lowerMessage.contains("quota") ||
        lowerMessage.contains("limit") ||
        lowerMessage.contains("credit") ||
        lowerMessage.contains("usage") ||
        lowerMessage.contains("monthly usage hard limit exceeded") ||
        lowerMessage.contains("platform-feature-disabled")||
        lowerMessage.contains("exceeded");
  }

  /**
   * 현재 API 키 상태 로깅
   */
  public void logCurrentKeyStatus() {
    log.info("현재 Apify API 키: {}번째 (총 {}개)",
        apifyApiConfig.getCurrentKeyIndex() + 1, MAX_RETRY_COUNT);
  }
}