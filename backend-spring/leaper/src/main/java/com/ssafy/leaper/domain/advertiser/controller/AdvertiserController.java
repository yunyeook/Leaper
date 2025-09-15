package com.ssafy.leaper.domain.advertiser.controller;

import com.ssafy.leaper.domain.advertiser.dto.request.AdvertiserSignupRequest;
import com.ssafy.leaper.domain.advertiser.dto.request.BusinessValidationApiRequest;
import com.ssafy.leaper.domain.advertiser.dto.response.AdvertiserSignupResponse;
import com.ssafy.leaper.domain.advertiser.service.AdvertiserService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Advertiser", description = "광고주 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/advertiser")
@RequiredArgsConstructor
public class AdvertiserController implements BaseController {

    private final AdvertiserService advertiserService;

    @Operation(summary = "광고주 회원가입", description = "광고주 계정을 생성합니다.")
    @PostMapping(value = "/signup", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AdvertiserSignupResponse>> signup(
            @Valid @ModelAttribute AdvertiserSignupRequest request) {

        log.info("Advertiser signup request - loginId: {}, brandName: {}",
                request.getLoginId(), request.getBrandName());

        return handle(advertiserService.signup(request));
    }

    @Operation(summary = "로그인 아이디 중복 검사", description = "광고주 로그인 아이디 중복을 검사합니다.")
    @GetMapping("/duplicate")
    public ResponseEntity<ApiResponse<Void>> checkLoginIdDuplicate(
            @RequestParam("loginId") String loginId) {

        log.info("Advertiser loginId duplicate check - loginId: {}", loginId);

        return handle(advertiserService.checkLoginIdDuplicate(loginId));
    }

    @Operation(summary = "사업자등록번호 검증", description = "국세청 API를 통해 사업자등록번호를 검증합니다.")
    @PostMapping("/business/validate")
    public ResponseEntity<ApiResponse<Void>> validateBusinessRegistration(
            @Valid @RequestBody BusinessValidationApiRequest request) {

        log.info("Business registration validation - businessRegNo: {}, representativeName: {}",
                request.getBusinessRegNo(), request.getRepresentativeName());

        return handle(advertiserService.validateBusinessRegistrationApi(request));
    }
}
