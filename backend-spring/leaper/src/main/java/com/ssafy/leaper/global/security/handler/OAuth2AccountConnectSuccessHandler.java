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
        connectData.put("provider", providerTypeId);
        connectData.putAll(extractedData);

        log.info("OAuth2 Account Connect Data - provider: {}, data: {}", providerTypeId, extractedData);

        // JSON 응답으로 계정 연결 정보 반환
        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>(ResponseStatus.SUCCESS, connectData);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private Map<String, Object> extractProviderData(OAuth2User oauth2User, String provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        Map<String, Object> extractedData = new HashMap<>();

        switch (provider.toLowerCase()) {
            case "google" -> {
                extractedData.put("external_account_id", attributes.get("email"));

                // YouTube 데이터를 원하는 형태로 변환
                if (attributes.containsKey("youtube_channels")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> channels = (java.util.List<Map<String, Object>>) attributes.get("youtube_channels");

                    java.util.List<Map<String, Object>> youtubeAccounts = new java.util.ArrayList<>();

                    for (Map<String, Object> channel : channels) {
                        Map<String, Object> account = new HashMap<>();

                        @SuppressWarnings("unchecked")
                        Map<String, Object> snippet = (Map<String, Object>) channel.get("snippet");
                        if (snippet != null) {
                            account.put("account_nickname", snippet.get("title"));

                            // 채널 URL 생성
                            String channelId = (String) channel.get("id");
                            String channelUrl = channelId != null ? "https://www.youtube.com/channel/" + channelId : "";
                            account.put("account_url", channelUrl);

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
                            account.put("account_profile_image_url", thumbnailUrl);
                        }

                        youtubeAccounts.add(account);
                    }

                    extractedData.put("YOUTUBE", youtubeAccounts);
                }
            }
            case "naver" -> {
                Map<String, Object> naverResponse = (Map<String, Object>) attributes.get("response");
                if (naverResponse != null) {
                    extractedData.put("providerMemberId", naverResponse.get("id"));
                    extractedData.put("email", naverResponse.get("email"));
                    extractedData.put("name", naverResponse.get("name"));
                    extractedData.put("nickname", naverResponse.get("nickname"));
                    extractedData.put("profileImage", naverResponse.get("profile_image"));
                    extractedData.put("age", naverResponse.get("age"));
                    extractedData.put("gender", naverResponse.get("gender"));
                    extractedData.put("birthday", naverResponse.get("birthday"));
                    extractedData.put("birthyear", naverResponse.get("birthyear"));
                    extractedData.put("mobile", naverResponse.get("mobile"));
                }
            }
            case "instagram" -> {
                extractedData.put("providerMemberId", attributes.get("id"));
                extractedData.put("username", attributes.get("username"));
                extractedData.put("accountType", attributes.get("account_type"));
                extractedData.put("mediaCount", attributes.get("media_count"));
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