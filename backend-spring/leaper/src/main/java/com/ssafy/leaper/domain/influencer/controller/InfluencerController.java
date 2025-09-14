package com.ssafy.leaper.domain.influencer.controller;

import com.ssafy.leaper.domain.influencer.dto.request.InfluencerSignupRequest;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerSignupResponse;
import com.ssafy.leaper.domain.influencer.service.InfluencerService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Influencer", description = "인플루언서 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/influencer")
@RequiredArgsConstructor
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
}