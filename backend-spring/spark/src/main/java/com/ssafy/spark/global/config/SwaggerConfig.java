package com.ssafy.spark.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor

@OpenAPIDefinition(
        info = @Info(title = "Leaper",
                description = "A302_SSAFY",
                version = "v1"),
        servers = {
                @Server(url = "https://j13a302.p.ssafy.io/api",
                        description = "운영 서버 URL"),
        }
)

public class SwaggerConfig {

}
