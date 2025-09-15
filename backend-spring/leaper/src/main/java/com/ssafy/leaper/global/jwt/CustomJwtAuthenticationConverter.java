package com.ssafy.leaper.global.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
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