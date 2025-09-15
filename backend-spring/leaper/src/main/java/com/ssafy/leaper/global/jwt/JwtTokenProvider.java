package com.ssafy.leaper.global.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${jwt.issuer}")
    private String issuer;


    /**
     * Influencer용 JWT 토큰 생성
     */
    public String generateInfluencerToken(String userId, String email) {
        log.info("Generating Influencer JWT token for userId: {}, email: {}", userId, email);
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(accessTokenExpiry, ChronoUnit.HOURS);

            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

            JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject(userId)
                    .issuedAt(now)
                    .expiresAt(expiry)
                    .claim("scope", "ROLE_INFLUENCER")
                    .claim("email", email)
                    .build();

            String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
            log.info("Influencer JWT token generated successfully for userId: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate Influencer JWT token for userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * Advertiser용 JWT 토큰 생성
     */
    public String generateAdvertiserToken(String userId, String loginId) {
        log.info("Generating Advertiser JWT token for userId: {}, loginId: {}", userId, loginId);
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(accessTokenExpiry, ChronoUnit.HOURS);

            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

            JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject(userId)
                    .issuedAt(now)
                    .expiresAt(expiry)
                    .claim("scope", "ROLE_ADVERTISER")
                    .claim("loginId", loginId)
                    .build();

            String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
            log.info("Advertiser JWT token generated successfully for userId: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate Advertiser JWT token for userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 회원가입용 임시 JWT 토큰 생성
     * providerMemberId와 providerTypeId를 포함한 등록 전용 토큰
     */
    public String generateRegistrationToken(String providerMemberId, String providerTypeId) {
        log.info("Generating registration token for provider: {}, providerMemberId: {}", providerTypeId, providerMemberId);
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(10, ChronoUnit.MINUTES); // 10분

            log.debug("Registration token expiry: {} (in 10 minutes)", expiry);

            JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

            JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject("REGISTRATION") // subject는 등록용으로 고정
                    .issuedAt(now)
                    .expiresAt(expiry)
                    .claim("providerMemberId", providerMemberId)
                    .claim("providerTypeId", providerTypeId)
                    .claim("tokenType", "REGISTRATION")
                    .build();

            log.debug("Registration JWT Claims: {}", claimsSet.getClaims());

            String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();

            log.info("Registration token generated successfully for provider: {}", providerTypeId);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate registration token for provider: {}, providerMemberId: {}",
                    providerTypeId, providerMemberId, e);
            throw e;
        }
    }
}