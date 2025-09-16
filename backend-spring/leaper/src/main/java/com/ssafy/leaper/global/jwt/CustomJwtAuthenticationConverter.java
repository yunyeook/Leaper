package com.ssafy.leaper.global.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 현재 요청 경로 확인
        String requestPath = getCurrentRequestPath();

        // tokenType 검증
        Object tokenTypeClaim = jwt.getClaim("tokenType");
        String tokenType = tokenTypeClaim != null ? tokenTypeClaim.toString() : null;

        // 회원가입 경로는 REGISTRATION 토큰 허용, 그 외는 ACCESS 토큰만 허용
        if ("/api/v1/influencer/signup".equals(requestPath)) {
            if (!"REGISTRATION".equals(tokenType)) {
                log.warn("Invalid tokenType for signup: {}. Only REGISTRATION tokens are allowed for signup", tokenType);
                OAuth2Error error = new OAuth2Error("invalid_token", "Invalid token type. Only REGISTRATION tokens are allowed for signup", null);
                throw new OAuth2AuthenticationException(error);
            }
        } else {
            if (!"ACCESS".equals(tokenType)) {
                log.warn("Invalid tokenType: {}. Only ACCESS tokens are allowed for API calls", tokenType);
                OAuth2Error error = new OAuth2Error("invalid_token", "Invalid token type. Only ACCESS tokens are allowed", null);
                throw new OAuth2AuthenticationException(error);
            }
        }

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private String getCurrentRequestPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getRequestURI();
        }
        return null;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // scope 클레임에서 권한 추출
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim != null) {
            String scope = scopeClaim.toString();
            log.debug("Extracting authorities from scope: {}", scope);

            // SCOPE_ROLE_ADVERTISER -> ROLE_ADVERTISER로 변환
            if (scope.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority(scope));
                log.debug("Added authority: {}", scope);
            }
        }

        log.info("Final authorities for JWT: {}", authorities);
        return authorities;
    }
}