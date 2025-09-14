package com.ssafy.leaper.global.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtValidationService {

    private final JwtDecoder jwtDecoder;

    public Jwt validateRegistrationToken(String authorizationHeader) {
        String token = extractTokenFromHeader(authorizationHeader);

        Jwt jwt = jwtDecoder.decode(token);

        validateRegistrationTokenClaims(jwt);

        return jwt;
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더가 올바르지 않습니다");
        }
        return authorizationHeader.substring(7);
    }

    private void validateRegistrationTokenClaims(Jwt jwt) {
        String tokenType = jwt.getClaimAsString("tokenType");
        String subject = jwt.getSubject();

        if (!"REGISTRATION".equals(tokenType) || !"REGISTRATION".equals(subject)) {
            throw new IllegalArgumentException("등록용 토큰이 아닙니다");
        }
    }
}