package com.ssafy.leaper.global.security.handler.test;

import com.ssafy.leaper.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class JwtTestController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/jwt")
    public String testJwtGeneration() {
        try {
            log.info("Starting JWT test generation...");
            String token = jwtTokenProvider.generateInfluencerToken("test-user-123", "test@example.com");
            log.info("JWT test generation successful!");
            log.info("JWT: " + jwtTokenProvider.generateAdvertiserToken("1", "asdfasdf"));
            return "JWT Generation Success: " + token.substring(0, 50) + "...";
        } catch (Exception e) {
            log.error("JWT test generation failed", e);
            return "JWT Generation Failed: " + e.getMessage();
        }
    }
}