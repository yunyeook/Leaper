package com.ssafy.leaper.global.config;

import com.ssafy.leaper.domain.auth.service.AdvertiserUserDetailsService;
import com.ssafy.leaper.global.jwt.CustomJwtAuthenticationConverter;
import com.ssafy.leaper.global.security.handler.OAuth2AccountConnectSuccessHandler;
import com.ssafy.leaper.global.security.handler.OAuth2AuthenticationFailureHandler;
import com.ssafy.leaper.global.security.handler.OAuth2AuthenticationSuccessHandler;
import com.ssafy.leaper.global.security.oauth2.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final OAuth2AccountConnectSuccessHandler oAuth2AccountConnectSuccessHandler;
    private final AdvertiserUserDetailsService advertiserUserDetailsService;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver;


    @Bean
    @Order(1)
    public SecurityFilterChain connectSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        http.securityMatcher("/connect/login/oauth2/**", "/connect/oauth2/authorization/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        http.oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .authorizationEndpoint(authorization ->
                                authorization.baseUri("/connect/oauth2/authorization"))
                        .redirectionEndpoint(redirection ->
                                redirection.baseUri("/connect/login/oauth2/code/*"))
                        .successHandler(oAuth2AccountConnectSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain oAuth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth ->
                        auth.authorizationRequestResolver(oAuth2AuthorizationRequestResolver))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler));

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));;

        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(HttpMethod.POST,
                                        "/api/v1/influencer/signup",
                                        "/api/v1/advertiser/signup",
                                        "/api/v1/advertiser/business/validate",
                                        "/api/v1/auth/advertiser/login",
                                        "/api/test/jwt/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/advertiser/duplicate", "/api/v1/influencer/duplicate").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(res ->
                        res.jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain defaultSecurity(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/auth-callback", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/ws/**").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }

//     security 꺼놓기
//     꺼놓으면 Authentication 등록이 안되어서 토큰에서 id를 못꺼내옴.
    @Bean
    @Order(0)
    public SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable());

        http.securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider advertiserAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(advertiserUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
