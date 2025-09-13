package com.ssafy.leaper.domain.insight.controller;

import com.ssafy.leaper.domain.insight.dto.response.*;
import com.ssafy.leaper.domain.insight.service.InsightService;
import com.ssafy.leaper.global.common.response.ServiceResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
@Tag(name = "Insight API", description = "인플루언서 인사이트 관련 API")
public class InsightController {

    private final InsightService insightService;

    @Operation(
        summary = "인플루언서 계정 인사이트 전체 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 반환합니다.")
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}")
    public ServiceResult<AccountInsightResponse> getAccountInsights(
        @PathVariable Long influencerId
    ) {
        return insightService.getAccountInsights(influencerId);
    }

    @Operation(
        summary = "인플루언서 계정 조회수만 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 조회수 데이터만 반환합니다.")
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}/view")
    public ServiceResult<InfluencerViewsResponse> getInfluencerViews(
        @PathVariable Long influencerId
    ) {
        return insightService.getInfluencerViews(influencerId);
    }
}
