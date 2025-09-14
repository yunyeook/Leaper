package com.ssafy.leaper.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${front.url}")
    private String frontUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 Authentication Failure: {}", exception.getMessage(), exception);

        String errorMessage = "소셜 로그인에 실패했습니다.";
        String errorCode = "AUTH_OAUTH2_FAILURE";

        // OAuth2 관련 예외에 따른 구체적인 에러 메시지 설정
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCodeFromOAuth2 = oauth2Exception.getError().getErrorCode();
            
            switch (errorCodeFromOAuth2) {
                case "invalid_request":
                    errorMessage = "잘못된 요청입니다.";
                    errorCode = "AUTH_INVALID_REQUEST";
                    break;
                case "unauthorized_client":
                    errorMessage = "인증되지 않은 클라이언트입니다.";
                    errorCode = "AUTH_UNAUTHORIZED_CLIENT";
                    break;
                case "access_denied":
                    errorMessage = "액세스가 거부되었습니다.";
                    errorCode = "AUTH_ACCESS_DENIED";
                    break;
                case "unsupported_response_type":
                    errorMessage = "지원하지 않는 응답 타입입니다.";
                    errorCode = "AUTH_UNSUPPORTED_RESPONSE_TYPE";
                    break;
                case "invalid_scope":
                    errorMessage = "잘못된 스코프입니다.";
                    errorCode = "AUTH_INVALID_SCOPE";
                    break;
                case "server_error":
                    errorMessage = "서버 오류가 발생했습니다.";
                    errorCode = "AUTH_SERVER_ERROR";
                    break;
                case "temporarily_unavailable":
                    errorMessage = "일시적으로 서비스를 사용할 수 없습니다.";
                    errorCode = "AUTH_TEMPORARILY_UNAVAILABLE";
                    break;
                default:
                    errorMessage = "알 수 없는 오류가 발생했습니다.";
                    errorCode = "AUTH_UNKNOWN_ERROR";
                    break;
            }
        }

        log.info("OAuth2 인증 실패 - errorCode: {}, errorMessage: {}", errorCode, errorMessage);

        // 프론트엔드 에러 페이지로 리다이렉트
        String redirectUrl = String.format("%s/auth/error?code=%s&message=%s",
                frontUrl,
                URLEncoder.encode(errorCode, StandardCharsets.UTF_8),
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));

        response.sendRedirect(redirectUrl);
    }
}