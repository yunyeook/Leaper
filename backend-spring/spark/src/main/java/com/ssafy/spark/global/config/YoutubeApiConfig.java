package com.ssafy.spark.global.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

@Configuration
@Data
@Slf4j
public class YoutubeApiConfig {

    @Value("${youtube.api.key1}")
    private String key1;

    @Value("${youtube.api.key2}")
    private String key2;

    @Value("${youtube.api.key3}")
    private String key3;

    private List<String> keys;

    @Value("${youtube.api.base-url}")
    private String baseUrl;

    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    @PostConstruct
    public void initKeys() {
        keys = Arrays.asList(key1, key2, key3);
        log.info("YouTube API 키 {}개 로드 완료", keys.size());
    }

    /**
     * 현재 사용 중인 API 키를 반환
     */
    public String getCurrentKey() {
        if (keys == null || keys.isEmpty()) {
            throw new RuntimeException("YouTube API 키가 설정되지 않았습니다.");
        }
        int index = currentKeyIndex.get() % keys.size();
        return keys.get(index);
    }

    /**
     * 다음 API 키로 전환
     */
    public String switchToNextKey() {
        if (keys == null || keys.isEmpty()) {
            throw new RuntimeException("YouTube API 키가 설정되지 않았습니다.");
        }

        int oldIndex = currentKeyIndex.get() % keys.size();
        int newIndex = currentKeyIndex.incrementAndGet() % keys.size();

        log.warn("YouTube API 키 전환: {}번째 키 -> {}번째 키", oldIndex + 1, newIndex + 1);

        return keys.get(newIndex);
    }

    /**
     * 사용 가능한 키 개수 반환
     */
    public int getKeyCount() {
        return keys != null ? keys.size() : 0;
    }

    @Bean
    public WebClient youtubeWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                        .build())
                .build();
    }
}
