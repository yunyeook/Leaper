package com.ssafy.leaper.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final SecretKey jwtSecretKey;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * JWT Access Token 생성
     */
    public String generateAccessToken(String userId, String role, String email) {
        log.info("Generating JWT token for userId: {}, role: {}, email: {}", userId, role, email);
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + accessTokenExpiry);

            log.debug("Token expiry: {} (in {} milliseconds)", expiry, accessTokenExpiry);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", role);
            claims.put("email", email);

            log.debug("JWT Claims: {}", claims);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(userId)
                    .issuer(issuer)
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(jwtSecretKey)
                    .compact();

            log.info("JWT token generated successfully for userId: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate JWT token for userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * Influencer용 JWT 토큰 생성
     */
    public String generateInfluencerToken(String userId, String email) {
        return generateAccessToken(userId, "ROLE_INFLUENCER", email);
    }

    /**
     * Advertiser용 JWT 토큰 생성
     */
    public String generateAdvertiserToken(String userId, String email) {
        return generateAccessToken(userId, "ROLE_ADVERTISER", email);
    }

    /**
     * 회원가입용 임시 JWT 토큰 생성
     * providerMemberId와 providerTypeId를 포함한 등록 전용 토큰
     */
    public String generateRegistrationToken(String providerMemberId, String providerTypeId) {
        log.info("Generating registration token for provider: {}, providerMemberId: {}", providerTypeId, providerMemberId);
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 600000); // 10분 (600,000ms)

            log.debug("Registration token expiry: {} (in 10 minutes)", expiry);

            Map<String, Object> claims = new HashMap<>();
            claims.put("providerMemberId", providerMemberId);
            claims.put("providerTypeId", providerTypeId);
            claims.put("tokenType", "REGISTRATION");

            log.debug("Registration JWT Claims: {}", claims);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject("REGISTRATION") // subject는 등록용으로 고정
                    .issuer(issuer)
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(jwtSecretKey)
                    .compact();

            log.info("Registration token generated successfully for provider: {}", providerTypeId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate registration token for provider: {}, providerMemberId: {}",
                    providerTypeId, providerMemberId, e);
            throw e;
        }
    }
}