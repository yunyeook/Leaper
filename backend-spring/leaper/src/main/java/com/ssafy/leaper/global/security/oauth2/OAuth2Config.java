package com.ssafy.leaper.global.security.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class OAuth2Config {

    @Bean
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        // 기본 Resolver (Spring Security 제공)
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
                return customizeRequest(req);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
                return customizeRequest(req);
            }

            private OAuth2AuthorizationRequest customizeRequest(OAuth2AuthorizationRequest req) {
                if (req == null) return null;

                Map<String, Object> extraParams = new HashMap<>(req.getAdditionalParameters());
                extraParams.put("prompt", "select_account"); // 👈 계정 선택 강제

                return OAuth2AuthorizationRequest.from(req)
                        .additionalParameters(extraParams)
                        .build();
            }
        };
    }
}

