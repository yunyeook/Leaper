package com.ssafy.leaper.domain.auth.controller;

import com.ssafy.leaper.domain.auth.dto.request.AdvertiserLoginRequest;
import com.ssafy.leaper.domain.auth.dto.response.AdvertiserLoginResponse;
import com.ssafy.leaper.domain.auth.service.AuthService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements BaseController {

    private final AuthService authService;

    @Operation(summary = "광고주 로그인", description = "광고주 로그인을 처리하고 JWT 토큰을 발급합니다.")
    @PostMapping("/advertiser/login")
    public ResponseEntity<ApiResponse<AdvertiserLoginResponse>> advertiserLogin(
            @Valid @RequestBody AdvertiserLoginRequest request) {

        log.info("Advertiser login request - loginId: {}", request.getLoginId());

        return handle(authService.advertiserLogin(request));
    }
}