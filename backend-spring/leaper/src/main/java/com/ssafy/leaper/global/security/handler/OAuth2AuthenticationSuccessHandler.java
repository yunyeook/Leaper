package com.ssafy.leaper.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.global.jwt.JwtTokenProvider;
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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${front.url}")
    private String frontUrl;
    private final JwtTokenProvider jwtTokenProvider;
    private final InfluencerRepository influencerRepository;
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
        String providerMemberId = extractProviderId(oAuth2User, provider); // 소셜 제공자별 고유 식별자
        String providerTypeId = provider.toUpperCase(); // 소셜 제공자 타입 (GOOGLE, NAVER, KAKAO)

        log.info("OAuth2 User Info - provider: {}, providerMemberId: {}, email: {}, name: {}",
                providerTypeId, providerMemberId, email, name);

        // DB에서 인플루언서 조회
        Optional<Influencer> influencerOpt = influencerRepository.findByProviderTypeIdAndProviderMemberIdAndIsDeletedFalse(providerTypeId, providerMemberId);

        if (influencerOpt.isEmpty()) {
            log.info("New user detected - redirecting for registration: provider={}, providerMemberId={}", providerTypeId, providerMemberId);

            // 등록용 JWT 토큰 생성
            String registrationToken = jwtTokenProvider.generateRegistrationToken(providerMemberId, providerTypeId);

            // 신규 사용자 - 등록용 토큰과 함께 리다이렉트
            String redirectUrl = frontUrl + "/auth/callback?" +
                    "isNew=true" +
                    "&registrationToken=" + URLEncoder.encode(registrationToken, StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
            return;
        }


        Influencer influencer = influencerOpt.get();
        String jwtToken = jwtTokenProvider.generateInfluencerToken(influencer.getInfluencerId().toString(), email);

        log.info("Generated JWT token for existing influencer: {}", influencer.getInfluencerId());

        // 기존 사용자 - JWT 토큰과 함께 리다이렉트
        String redirectUrl = frontUrl + "/auth/callback?" +
                "isNew=false" +
                "&influencerId=" + influencer.getInfluencerId() +
                "&token=" + URLEncoder.encode(jwtToken, StandardCharsets.UTF_8);

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