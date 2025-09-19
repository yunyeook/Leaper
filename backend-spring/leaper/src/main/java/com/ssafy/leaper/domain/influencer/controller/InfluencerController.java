package com.ssafy.leaper.domain.influencer.controller;

import com.ssafy.leaper.domain.influencer.dto.request.InfluencerSignupRequest;
import com.ssafy.leaper.domain.influencer.dto.request.InfluencerUpdateRequest;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerProfileResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerPublicProfileResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerUpdateResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerSignupResponse;
import com.ssafy.leaper.domain.influencer.service.InfluencerService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Influencer", description = "인플루언서 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/influencer")
@RequiredArgsConstructor
@Validated
public class InfluencerController implements BaseController {

    private final InfluencerService influencerService;

    @Operation(summary = "인플루언서 회원가입", description = "소셜 로그인 후 인플루언서 계정을 생성합니다.")
    @PostMapping(value = "/signup", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<InfluencerSignupResponse>> signup(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @ModelAttribute InfluencerSignupRequest request) {

        log.info("Influencer signup request - nickname: {}, email: {}",
                request.getNickname(), request.getEmail());

        return handle(influencerService.signup(request, authorizationHeader));
    }

    @Operation(summary = "인플루언서 닉네임 중복 검사", description = "인플루언서 회원가입 시 닉네임 중복 여부를 확인합니다.")
    @GetMapping("/duplicate")
    public ResponseEntity<ApiResponse<Void>> checkNicknameDuplicate(
            @RequestParam("nickname")
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname) {

        log.info("Nickname duplicate check requested - nickname: {}", nickname);

        return handle(influencerService.checkNicknameDuplicate(nickname));
    }

    @Operation(summary = "인플루언서 내 프로필 조회", description = "로그인한 인플루언서의 프로필 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<InfluencerProfileResponse>> getMyProfile(Authentication authentication) {

        Integer influencerId = Integer.valueOf(authentication.getName());

        log.info("My profile request - influencerId: {}", influencerId);

        return handle(influencerService.getMyProfile(influencerId));
    }

    @Operation(summary = "다른 인플루언서 프로필 조회", description = "인플루언서 ID로 다른 인플루언서의 공개 프로필 정보를 조회합니다.")
    @GetMapping("/{influencerId}")
    public ResponseEntity<ApiResponse<InfluencerPublicProfileResponse>> getPublicProfile(
            @PathVariable Integer influencerId) {

        log.info("Public profile request - influencerId: {}", influencerId);

        return handle(influencerService.getPublicProfile(influencerId));
    }

    @Operation(summary = "인플루언서 회원탈퇴", description = "로그인한 인플루언서의 계정을 탈퇴(soft delete)합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication authentication) {

        Integer influencerId = Integer.valueOf(authentication.getName());

        log.info("Account deletion request - influencerId: {}", influencerId);

        return handle(influencerService.deleteAccount(influencerId));
    }

    @Operation(summary = "인플루언서 프로필 수정", description = "로그인한 인플루언서의 프로필 정보를 수정합니다.")
    @PatchMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<InfluencerUpdateResponse>> updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute InfluencerUpdateRequest request) {

        Integer influencerId = Integer.valueOf(authentication.getName());

        log.info("Profile update request - influencerId: {}, nickname: {}",
                influencerId, request.getNickname());

        return handle(influencerService.updateProfile(influencerId, request));
    }

    @Operation(summary = "개발용 계정 삭제", description = "인플루언서 계정을 이메일로 검색해 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccountByEmail(@RequestParam String email) {

        return handle(influencerService.deleteAccountByEmail(email));
    }
}