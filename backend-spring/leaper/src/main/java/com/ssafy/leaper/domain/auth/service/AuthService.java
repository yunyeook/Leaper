package com.ssafy.leaper.domain.auth.service;

import com.ssafy.leaper.domain.auth.dto.request.AdvertiserLoginRequest;
import com.ssafy.leaper.domain.auth.dto.response.AdvertiserLoginResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public ServiceResult<AdvertiserLoginResponse> advertiserLogin(AdvertiserLoginRequest request) {
        log.info("Starting advertiser login - loginId: {}", request.getLoginId());

        try {
            // Spring Security Authentication 사용
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );

            // 인증 성공 - JWT 토큰 생성
            String userId = authentication.getName(); // Principal의 이름 (advertiser ID)
            String accessToken = jwtTokenProvider.generateAdvertiserToken(userId, request.getLoginId());

            AdvertiserLoginResponse response = AdvertiserLoginResponse.builder()
                    .advertiserId(userId)
                    .accessToken(accessToken)
                    .build();

            log.info("Advertiser login successful - advertiserId: {}, loginId: {}",
                    userId, request.getLoginId());

            return ServiceResult.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Advertiser login failed - invalid credentials: loginId: {}", request.getLoginId());
            return ServiceResult.fail(ErrorCode.USER_LOGIN_FAIL);
        } catch (AuthenticationException e) {
            log.warn("Advertiser login failed - authentication error: loginId: {}, error: {}",
                    request.getLoginId(), e.getMessage());
            return ServiceResult.fail(ErrorCode.USER_LOGIN_FAIL);
        } catch (Exception e) {
            log.error("Failed to login advertiser - loginId: {}", request.getLoginId(), e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }
}