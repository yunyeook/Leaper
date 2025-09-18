package com.ssafy.leaper.global.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User user = super.loadUser(userRequest);

        // YouTube scope 체크
        Set<String> scopes = userRequest.getAccessToken().getScopes();
        boolean hasYouTubeScope = scopes.contains("https://www.googleapis.com/auth/youtube.readonly");

        log.info("OAuth2UserRequest - scopes: {}, hasYouTubeScope: {}", scopes, hasYouTubeScope);

        if (hasYouTubeScope) {
            String accessToken = userRequest.getAccessToken().getTokenValue();
            Map<String, Object> youtubeData = fetchYouTubeChannelData(accessToken);
            return new CustomOAuth2User(user, youtubeData);
        }

        return user;
    }

    private Map<String, Object> fetchYouTubeChannelData(String accessToken) {
        Map<String, Object> youtubeData = new HashMap<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&mine=true";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("items")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) response.getBody().get("items");

                youtubeData.put("youtube_channels", items);
            }

            log.info("YouTube channel data fetched successfully: {}", youtubeData);

        } catch (Exception e) {
            log.error("Failed to fetch YouTube channel data", e);
        }

        return youtubeData;
    }
}