package com.ssafy.leaper.global.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InstagramTokenResponseConverter implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> tokenResponseParameters) {
        log.info("Converting token response: {}", tokenResponseParameters);

        // Instagram 토큰인지 확인 (user_id 필드가 있고 token_type이 없으면 Instagram)
        boolean isInstagramToken = tokenResponseParameters.containsKey("user_id") &&
                                  !tokenResponseParameters.containsKey("token_type");

        if (!isInstagramToken) {
            // Instagram이 아닌 경우 기본 처리 (Google, Naver 등)
            log.info("Non-Instagram token detected, using default processing");
            return convertStandardToken(tokenResponseParameters);
        }

        log.info("Instagram token detected, using custom processing");

        String accessToken = (String) tokenResponseParameters.get(OAuth2ParameterNames.ACCESS_TOKEN);
        if (accessToken == null) {
            log.error("No access_token found in response");
            return null;
        }

        // Instagram은 token_type을 반환하지 않으므로 기본값 설정
        OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER;

        // Instagram은 expires_in을 반환하지 않지만, 토큰은 60일간 유효
        // 하지만 Spring Security는 만료 시간이 필요하므로 긴 시간으로 설정
        long expiresIn = 5184000L; // 60일 (60 * 24 * 60 * 60 초)

        // Instagram 특화 추가 파라미터들
        Map<String, Object> additionalParameters = new HashMap<>();
        if (tokenResponseParameters.containsKey("user_id")) {
            additionalParameters.put("user_id", tokenResponseParameters.get("user_id"));
        }

        return OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .additionalParameters(additionalParameters)
                .build();
    }

    private OAuth2AccessTokenResponse convertStandardToken(Map<String, Object> tokenResponseParameters) {
        String accessToken = (String) tokenResponseParameters.get(OAuth2ParameterNames.ACCESS_TOKEN);
        if (accessToken == null) {
            log.error("No access_token found in response");
            return null;
        }

        // token_type 처리
        String tokenTypeValue = (String) tokenResponseParameters.get(OAuth2ParameterNames.TOKEN_TYPE);
        OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER;
        if ("bearer".equalsIgnoreCase(tokenTypeValue)) {
            tokenType = OAuth2AccessToken.TokenType.BEARER;
        }

        // expires_in 처리
        long expiresIn = 3600L; // 기본값 1시간
        if (tokenResponseParameters.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
            Object expiresInObj = tokenResponseParameters.get(OAuth2ParameterNames.EXPIRES_IN);
            if (expiresInObj instanceof Number) {
                expiresIn = ((Number) expiresInObj).longValue();
            }
        }

        // 추가 파라미터들
        Map<String, Object> additionalParameters = new HashMap<>();
        for (Map.Entry<String, Object> entry : tokenResponseParameters.entrySet()) {
            if (!OAuth2ParameterNames.ACCESS_TOKEN.equals(entry.getKey()) &&
                !OAuth2ParameterNames.TOKEN_TYPE.equals(entry.getKey()) &&
                !OAuth2ParameterNames.EXPIRES_IN.equals(entry.getKey())) {
                additionalParameters.put(entry.getKey(), entry.getValue());
            }
        }

        return OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .additionalParameters(additionalParameters)
                .build();
    }
}