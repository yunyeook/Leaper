package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseApifyService {
  @Autowired
  protected  WebClient webClient;
  @Autowired
  protected  ObjectMapper objectMapper;

  @Value("${apify.api-key}")
  protected String apiKey;

  @Value("${apify.base-url}")
  protected String baseUrl; // https://api.apify.com/v2


  /**
   * Apify Actor 실행
   */
  protected String runActor(String actorId, Map<String, Object> input) {
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
   * Actor 실행 상태 확인
   */
  protected String checkRunStatus(String runId) {
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
  protected String getRunResults(String runId) {
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


}