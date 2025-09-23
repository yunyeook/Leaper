package com.ssafy.leaper.global.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.data.mongodb.host:mongodb}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port:27017}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database:leaper}")
    private String databaseName;

    @Value("${spring.data.mongodb.username:}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @NotNull
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        String connectionString;

        // 사용자명과 비밀번호가 있는 경우
        if (!username.isEmpty() && !password.isEmpty()) {
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s?authSource=admin",
                    username, password, mongoHost, mongoPort, databaseName);
        } else {
            // 인증 없는 경우
            connectionString = String.format("mongodb://%s:%d/%s",
                    mongoHost, mongoPort, databaseName);
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();

        return MongoClients.create(settings);
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