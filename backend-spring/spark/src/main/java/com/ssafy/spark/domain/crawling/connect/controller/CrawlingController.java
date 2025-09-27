package com.ssafy.spark.domain.crawling.connect.controller;

import com.ssafy.spark.domain.crawling.connect.request.CrawlingRequest;
import com.ssafy.spark.domain.crawling.connect.service.CrawlingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crawling")
@Slf4j
public class CrawlingController{

  @Autowired
  private CrawlingService crawlingService;
  /**
   * 계정 연결 시 크롤링 & 인사이트 저장 시작하는 메서드
   */
  @Value("${spark.api.key}")
  private String expectedApiKey; //leaper와 소통하는 key

  @PostMapping("/start")
  public ResponseEntity<Boolean> startCrawling(
      @RequestBody CrawlingRequest crawlingRequest,
      @RequestHeader("X-API-Key") String apiKey) {

    try {
      // API 키 검증
      if (!expectedApiKey.equals(apiKey)) {
        log.warn("Invalid API key received: {}", apiKey);
        return ResponseEntity.status(401).body(false);
      }

      log.info("크롤링 요청 수신 - 계정 ID: {}, 플랫폼: {}",
          crawlingRequest.getPlatformAccountId(), crawlingRequest.getPlatformTypeId());

      // 크롤링 서비스 호출
      crawlingService.startCrawlingAsync(crawlingRequest);

      return ResponseEntity.ok(true);

    } catch (Exception e) {
      log.error("크롤링 요청 처리 실패", e);
      return ResponseEntity.ok(false);
    }
  }
}