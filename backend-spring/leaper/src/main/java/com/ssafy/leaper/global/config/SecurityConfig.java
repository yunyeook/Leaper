package com.ssafy.leaper.global.config;

import com.ssafy.leaper.global.security.handler.OAuth2AuthenticationFailureHandler;
import com.ssafy.leaper.global.security.handler.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable());

        http.authorizeHttpRequests(
                c -> c.requestMatchers("/home").permitAll()
                        .anyRequest().authenticated()
        );

        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                        .baseUri("/api/v1/auth/login"))
                .redirectionEndpoint(redirection -> redirection
                        .baseUri("/login/oauth2/code/*"))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
        );


        return http.build();
    }

}
