package com.ssafy.leaper.global.jwt;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    // HS256 서명용 SecretKey Bean 등록
    @Bean
    public SecretKey jwtSecretKey() {
        // JJWT의 Keys.hmacShaKeyFor() 사용 → 길이 검증 + 안전한 SecretKey 생성
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 선택: SignatureAlgorithm Bean (필요하다면 주입해서 사용)
    @Bean
    public SignatureAlgorithm jwtSignatureAlgorithm() {
        return SignatureAlgorithm.HS256;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

}
