package com.ssafy.leaper.global.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class InstagramTokenResponseConverter implements Converter<Object, OAuth2AccessTokenResponse> {

    @Override
    public OAuth2AccessTokenResponse convert(Object tokenResponseParameters) {
        log.info("=== Instagram Token Response Converter ===");
        log.info("Input object type: {}", tokenResponseParameters.getClass().getName());
        log.info("Input object content: {}", tokenResponseParameters);

        // 입력 객체를 Map으로 변환
        Map<String, Object> parameters = convertToMap(tokenResponseParameters);
        log.info("Converted parameters: {}", parameters);

        String accessToken = extractStringValue(parameters, OAuth2ParameterNames.ACCESS_TOKEN);
        if (accessToken == null) {
            log.error("No access_token found in response");
            return null;
        }

        log.info("Extracted access_token: {}...", accessToken.substring(0, Math.min(10, accessToken.length())));

        // Instagram은 token_type을 반환하지 않으므로 기본값 설정
        OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER;

        // Instagram은 expires_in을 반환하지 않지만, 토큰은 60일간 유효
        // 하지만 Spring Security는 만료 시간이 필요하므로 긴 시간으로 설정
        long expiresIn = 5184000L; // 60일 (60 * 24 * 60 * 60 초)

        // Instagram 특화 추가 파라미터들
        Map<String, Object> additionalParameters = new HashMap<>();
        String userId = extractStringValue(parameters, "user_id");
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

    /**
     * 다양한 타입의 입력 객체를 Map으로 변환
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object input) {
        log.info("Converting object to map. Input type: {}", input.getClass().getName());

        if (input instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) input;
            log.info("Input is already a Map with {} entries", rawMap.size());

            if (rawMap instanceof MultiValueMap) {
                log.info("Input is MultiValueMap");
                MultiValueMap<String, String> multiValueMap = (MultiValueMap<String, String>) rawMap;
                Map<String, Object> result = new HashMap<>();

                for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    log.info("MultiValueMap entry - key: {}, values: {}", key, values);

                    // 첫 번째 값만 사용
                    if (values != null && !values.isEmpty()) {
                        result.put(key, values.get(0));
                    }
                }

                log.info("Converted MultiValueMap to Map: {}", result);
                return result;
            } else {
                log.info("Input is regular Map");
                return (Map<String, Object>) rawMap;
            }
        }

        log.error("Unsupported input type: {}", input.getClass().getName());
        throw new IllegalArgumentException("Unsupported input type: " + input.getClass().getName());
    }
}