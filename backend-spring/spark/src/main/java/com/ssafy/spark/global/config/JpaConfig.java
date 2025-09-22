package com.ssafy.spark.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing  // 이 어노테이션이 있는지 확인
public class JpaConfig {
}