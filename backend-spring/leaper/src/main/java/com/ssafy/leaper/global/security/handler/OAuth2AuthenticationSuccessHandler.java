package com.ssafy.leaper.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${front.url}")
    private String frontUrl;

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("OAuth2 Authentication Success");

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauth2Token.getPrincipal();

        // 소셜 제공자 정보 추출
        String provider = oauth2Token.getAuthorizedClientRegistrationId();

        // OAuth2User에서 사용자 정보 추출
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = extractProviderId(oAuth2User, provider);

        log.info("OAuth2 User Info - provider: {}, providerId: {}, email: {}, name: {}",
                provider, providerId, email, name);

        // 간단한 성공 리다이렉트 (테스트용)
        String redirectUrl = frontUrl + "/success?" +
            URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }

    private String extractProviderId(OAuth2User oauth2User, String provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("sub");
            case "naver" -> {
                Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
                yield naverResponse != null ? (String) naverResponse.get("id") : null;
            }
            case "kakao" -> {
                Object id = attributes.get("id");
                yield id != null ? id.toString() : null;
            }
            default -> {
                log.warn("Unknown OAuth2 provider: {}", provider);
                yield (String) attributes.get("id");
            }
        };
    }
}