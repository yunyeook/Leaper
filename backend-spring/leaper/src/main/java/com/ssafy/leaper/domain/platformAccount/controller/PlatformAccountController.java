package com.ssafy.leaper.domain.platformAccount.controller;

import com.ssafy.leaper.domain.platformAccount.dto.request.PlatformAccountCreateRequest;
import com.ssafy.leaper.domain.platformAccount.dto.response.PlatformAccountResponse;
import com.ssafy.leaper.domain.platformAccount.service.PlatformAccountService;
import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.ResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.Handle;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platformAccount")
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;
    //TODO : 삭제 예정 =>요청 테스트
    @GetMapping("/connect/test")
    public ResponseEntity<String> connectSuccess(){
        platformAccountService.connect();
        return ResponseEntity.ok("connectOk");
    }



    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPlatformAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody List<PlatformAccountCreateRequest> requests) {

        log.info("플랫폼 계정 등록 요청 - 사용자: {}, 계정 수: {}", jwt.getSubject(), requests.size());

        Integer influencerId = Integer.valueOf(jwt.getSubject());
        platformAccountService.createPlatformAccounts(influencerId, requests);

        return ResponseEntity.ok(new ApiResponse<>(ResponseStatus.SUCCESS, Map.of()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlatformAccountResponse>>> getPlatformAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("플랫폼 계정 조회 요청 - 사용자: {}", jwt.getSubject());

        Integer influencerId = Integer.valueOf(jwt.getSubject());
        List<PlatformAccountResponse> platformAccounts = platformAccountService.getPlatformAccountsByInfluencer(influencerId);

        return ResponseEntity.ok(new ApiResponse<>(ResponseStatus.SUCCESS, platformAccounts));
    }

    @GetMapping("/{platformAccountId}")
    public ResponseEntity<ApiResponse<PlatformAccountResponse>> getPlatformAccount(
            @PathVariable Integer platformAccountId) {

        log.info("개별 플랫폼 계정 조회 요청 - 계정 ID: {}", platformAccountId);

        PlatformAccountResponse platformAccount = platformAccountService.getPlatformAccountById(platformAccountId);

        return ResponseEntity.ok(new ApiResponse<>(ResponseStatus.SUCCESS, platformAccount));
    }

    @DeleteMapping("/{platformAccountId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePlatformAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer platformAccountId) {

        log.info("플랫폼 계정 삭제 요청 - 사용자: {}, 계정 ID: {}", jwt.getSubject(), platformAccountId);

        Integer influencerId = Integer.valueOf(jwt.getSubject());
        platformAccountService.deletePlatformAccount(influencerId, platformAccountId);

        return ResponseEntity.ok(new ApiResponse<>(ResponseStatus.SUCCESS, Map.of()));
    }
}