package com.ssafy.leaper.global.security.handler.test;

import com.ssafy.leaper.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test/jwt")
@RequiredArgsConstructor
public class JWTController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 테스트용 JWT 토큰 생성 API
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestToken(
            @RequestParam String userId,
            @RequestParam String role,
            @RequestParam String email) {

        log.info("Test JWT token generation request - userId: {}, role: {}, email: {}", userId, role, email);

        try {
            String token;

            if ("INFLUENCER".equals(role)) {
                token = jwtTokenProvider.generateInfluencerToken(userId, email);
            } else if ("ADVERTISER".equals(role)) {
                token = jwtTokenProvider.generateAdvertiserToken(userId, email);
            } else {
                log.warn("Invalid role provided: {}", role);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("message", "Invalid role. Must be INFLUENCER or ADVERTISER");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("token", token);
            response.put("userId", userId);
            response.put("role", role);
            response.put("email", email);

            log.info("Test JWT token generated successfully for userId: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate test JWT token for userId: {}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to generate JWT token: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}