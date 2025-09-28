package com.ssafy.spark.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class ApifyApiConfig {

  @Value("${apify.api.key1}") private String key1;
  @Value("${apify.api.key2}") private String key2;
  @Value("${apify.api.key3}") private String key3;
  @Value("${apify.api.key4}") private String key4;
  @Value("${apify.api.key5}") private String key5;
  @Value("${apify.api.key6}") private String key6;
  @Value("${apify.api.key7}") private String key7;
  @Value("${apify.api.key8}") private String key8;
  @Value("${apify.api.key9}") private String key9;
  @Value("${apify.api.key10}") private String key10;
  @Value("${apify.api.key11}") private String key11;
  @Value("${apify.api.key12}") private String key12;

  private List<String> keys;
  private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

  @PostConstruct
  public void initKeys() {
    keys = Arrays.asList(key1, key2, key3, key4, key5, key6, key7, key8, key9, key10, key11, key12);
    log.info("Apify API 키 {}개 로드 완료", keys.size());
  }

  /**
   * 현재 사용 중인 API 키를 반환
   */
  public String getCurrentKey() {
    if (keys == null || keys.isEmpty()) {
      throw new RuntimeException("Apify API 키가 설정되지 않았습니다.");
    }
    int index = currentKeyIndex.get() % keys.size();
    return keys.get(index);
  }

  /**
   * 다음 API 키로 전환
   */
  public String switchToNextKey() {
    if (keys == null || keys.isEmpty()) {
      throw new RuntimeException("Apify API 키가 설정되지 않았습니다.");
    }

    int oldIndex = currentKeyIndex.get() % keys.size();
    int newIndex = currentKeyIndex.incrementAndGet() % keys.size();

    log.warn("Apify API 키 전환: {}번째 키 -> {}번째 키", oldIndex + 1, newIndex + 1);

    return keys.get(newIndex);
  }

  /**
   * 현재 키 인덱스 반환 (0부터 시작)
   */
  public int getCurrentKeyIndex() {
    return currentKeyIndex.get() % keys.size();
  }

  /**
   * 사용 가능한 키 개수 반환
   */
  public int getKeyCount() {
    return keys != null ? keys.size() : 0;
  }

  /**
   * 특정 키 인덱스로 설정 (테스트용)
   */
  public void setCurrentKeyIndex(int index) {
    if (index >= 0 && index < keys.size()) {
      currentKeyIndex.set(index);
      log.info("Apify API 키 인덱스 설정: {}번째 키", index + 1);
    } else {
      throw new IllegalArgumentException("잘못된 키 인덱스: " + index + " (유효 범위: 0-" + (keys.size() - 1) + ")");
    }
  }

  /**
   * 현재 키 상태 로깅
   */
  public void logCurrentKeyStatus() {
    log.info("현재 Apify API 키: {}번째 (총 {}개)",
        getCurrentKeyIndex() + 1, getKeyCount());
  }
}