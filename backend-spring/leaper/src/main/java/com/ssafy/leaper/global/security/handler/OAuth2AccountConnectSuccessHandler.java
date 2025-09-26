package com.ssafy.leaper.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AccountConnectSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("OAuth2 Account Connect Success");

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauth2Token.getPrincipal();

        // 소셜 제공자 정보 추출
        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
        String provider = extractProviderFromRegistrationId(registrationId);
        String providerTypeId = provider.toUpperCase();

        log.info("OAuth2 Account Connect - provider: {}", providerTypeId);

        // Provider별로 데이터 추출
        Map<String, Object> extractedData = extractProviderData(oAuth2User, provider);

        // 응답 데이터 구성
        Map<String, Object> connectData = new HashMap<>();
        connectData.putAll(extractedData);

        log.info("OAuth2 Account Connect Data - provider: {}, data: {}", providerTypeId, extractedData);

        // HTML 응답으로 팝업 닫기 및 부모 창에 데이터 전송
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>(ResponseStatus.SUCCESS, connectData);
        String jsonData = objectMapper.writeValueAsString(apiResponse);

        String htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>계정 연결 완료</title>
            </head>
            <body>
                <script>
                    const data = %s;
                    if (window.opener) {
                        window.opener.postMessage({
                            type: 'OAUTH_SUCCESS',
                            data: data
                        }, '*');
                        window.close();
                    } else {
                        console.error('부모 창을 찾을 수 없습니다.');
                    }
                </script>
            </body>
            </html>
            """.formatted(jsonData);

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(htmlResponse);
    }

    private Map<String, Object> extractProviderData(OAuth2User oauth2User, String provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        Map<String, Object> extractedData = new HashMap<>();

        switch (provider.toLowerCase()) {
            case "google" -> {
                // YouTube 데이터를 단일 객체로 변환
                if (attributes.containsKey("youtube_channels")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> channels = (java.util.List<Map<String, Object>>) attributes.get("youtube_channels");

                    // 첫 번째(선택된) 채널만 처리
                    if (!channels.isEmpty()) {
                        Map<String, Object> channel = channels.get(0);

                        // 채널 ID를 externalAccountId로 사용
                        String channelId = (String) channel.get("id");
                        extractedData.put("externalAccountId", channelId);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> snippet = (Map<String, Object>) channel.get("snippet");
                        if (snippet != null) {
                            // customUrl을 accountNickname으로 사용
                            String customUrl = (String) snippet.get("customUrl");
                            log.info("Debug - customUrl: '{}', title: '{}', customUrl is null: {}", customUrl, snippet.get("title"), customUrl == null);
                            extractedData.put("accountNickname", customUrl != null ? customUrl : snippet.get("title"));

                            // 채널 URL 생성
                            String channelUrl = channelId != null ? "https://www.youtube.com/channel/" + channelId : "";
                            extractedData.put("accountUrl", channelUrl);

                            // 썸네일 URL 추출
                            @SuppressWarnings("unchecked")
                            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                            String thumbnailUrl = "";
                            if (thumbnails != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> defaultThumbnail = (Map<String, Object>) thumbnails.get("default");
                                if (defaultThumbnail != null) {
                                    thumbnailUrl = (String) defaultThumbnail.getOrDefault("url", "");
                                }
                            }
                            extractedData.put("accountProfileImageUrl", thumbnailUrl);
                        }

                        extractedData.put("platformTypeId", "YOUTUBE");
                    }
                }
            }
            case "naver" -> {
                Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
                if (naverResponse != null) {
                    extractedData.put("externalAccountId", naverResponse.get("email"));
                    extractedData.put("accountNickname", naverResponse.get("nickname"));
                    extractedData.put("accountUrl", ""); // Naver는 직접적인 프로필 URL이 없음
                    extractedData.put("accountProfileImageUrl", naverResponse.get("profile_image"));
                }
                extractedData.put("platformTypeId", "NAVER");
            }
            case "instagram" -> {
                String username = (String) attributes.get("username");
                extractedData.put("externalAccountId", attributes.get("id"));
                extractedData.put("accountNickname", username);
                extractedData.put("accountUrl", username != null ? "https://www.instagram.com/" + username : "");
                extractedData.put("accountProfileImageUrl", ""); // Instagram Graph API에서는 기본적으로 프로필 이미지를 제공하지 않음
                extractedData.put("platformTypeId", "INSTAGRAM");
            }
            default -> {
                log.warn("Unknown OAuth2 provider: {}", provider);
                extractedData.put("providerMemberId", attributes.get("id"));
                extractedData.putAll(attributes);
            }
        }

        return extractedData;
    }

    private String extractProviderFromRegistrationId(String registrationId) {
        // "google-connect" -> "google", "naver-connect" -> "naver" 등
        if (registrationId.endsWith("-connect")) {
            return registrationId.substring(0, registrationId.length() - "-connect".length());
        }
        return registrationId;
    }
}