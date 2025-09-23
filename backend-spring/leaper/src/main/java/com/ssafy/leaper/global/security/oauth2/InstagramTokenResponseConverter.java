package com.ssafy.leaper.global.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InstagramTokenResponseConverter implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    @Override
    public OAuth2AccessTokenResponse convert(Map<String, Object> tokenResponseParameters) {
        log.info("Converting Instagram token response: {}", tokenResponseParameters);

        String accessToken = extractStringValue(tokenResponseParameters, OAuth2ParameterNames.ACCESS_TOKEN);
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
        String userId = extractStringValue(tokenResponseParameters, "user_id");
        if (userId != null) {
            additionalParameters.put("user_id", userId);
        }

        return OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .additionalParameters(additionalParameters)
                .build();
    }

    /**
     * form-encoded 응답에서 String 값을 안전하게 추출
     * LinkedMultiValueMap의 경우 값이 List<String> 형태로 저장됨
     */
    private String extractStringValue(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }

        // LinkedMultiValueMap의 경우 List<String> 형태
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) value;
            return list.isEmpty() ? null : list.get(0);
        }

        // 일반적인 경우 String 형태
        return value.toString();
    }
}