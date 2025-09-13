package com.ssafy.leaper.global.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZoneId;
import java.util.Optional;

@Configuration
@EnableMongoRepositories(basePackages = "com.ssafy.leaper.domain.*.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @NotNull
    @Override
    protected String getDatabaseName() {
        return "leaper";
    }

    // Spring Security 없이 사용
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }

    // Spring Security 사용
//    @Bean
//    public AuditorAware<String> auditorProvider() {
//        return () -> {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//            if (authentication == null || !authentication.isAuthenticated()) {
//                return Optional.empty();
//            }
//
//            return Optional.of(authentication.getName());
//        };
//    }
}