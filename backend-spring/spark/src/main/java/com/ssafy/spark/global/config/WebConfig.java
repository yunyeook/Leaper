package com.ssafy.spark.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Bean
  public WebClient webClient() {
    return WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .build();
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    // 30분으로 timeout 설정
    configurer.setDefaultTimeout(30 * 60 * 1000L); // 30분 = 30 * 60 * 1000 밀리초
  }
}