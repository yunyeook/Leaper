package com.ssafy.leaper.domain.insight.controller;

import com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight.AccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight.InfluencerViewsResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight.TypeInsightResponse;
import com.ssafy.leaper.domain.insight.service.DailyAccountInsightService;
import com.ssafy.leaper.domain.insight.service.DailyTypeInsightService;
import com.ssafy.leaper.global.common.response.ServiceResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
@Tag(name = "Insight API", description = "인플루언서 인사이트 관련 API")
public class InsightController {

    private final DailyAccountInsightService dailyAccountInsightService;
    private final DailyTypeInsightService dailyTypeInsightService;


    @Operation(
        summary = "인플루언서 계정 인사이트 전체 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}")
    public ServiceResult<AccountInsightResponse> getAccountInsights(
        @PathVariable Long influencerId
    ) {
        return dailyAccountInsightService.getAccountInsights(influencerId);
    }

    @Operation(
        summary = "인플루언서 계정 조회수만 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 조회수를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}/view")
    public ServiceResult<InfluencerViewsResponse> getInfluencerViews(
        @PathVariable Long influencerId
    ) {
        return dailyAccountInsightService.getInfluencerViews(influencerId);
    }

    @Operation(
        summary = "플랫폼 계정 인사이트 전체 조회",
        description = "특정 플랫폼 계정에 대해 일별 및 월별 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/platformAccount/{platformAccountId}")
    public ServiceResult<AccountInsightResponse> getPlatformAccountInsights(
        @PathVariable Long platformAccountId
    ) {
        return dailyAccountInsightService.getPlatformAccountInsights(platformAccountId);
    }

    @Operation(
        summary = "플랫폼 계정 조회수만 조회",
        description = "특정 플랫폼 계정에 대해 일별 및 월별 조회수를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/platformAccount/{platformAccountId}/view")
    public ServiceResult<InfluencerViewsResponse> getPlatformAccountViews(
        @PathVariable Long platformAccountId
    ) {
        return dailyAccountInsightService.getPlatformAccountViews(platformAccountId);
    }

    @Operation(
        summary = "인플루언서 타입 인사이트 조회",
        description = "특정 인플루언서의 계정별로 특정 ContentType(영상, 쇼츠, 포스트 등)의 일별 및 월별 인사이트를 조회합니다."
    )
    @GetMapping("/dailyTypeInsight/influencer/{influencerId}")
    public ServiceResult<TypeInsightResponse> getTypeInsightsByInfluencer(
        @PathVariable Long influencerId,
        @RequestParam("contentType") String contentTypeId
    ) {
        return dailyTypeInsightService.getTypeInsightsByInfluencer(influencerId, contentTypeId);
    }

    @Operation(
        summary = "플랫폼 계정 타입 인사이트 조회",
        description = "특정 플랫폼 계정에 대해 특정 ContentType의 일별 및 월별 인사이트를 조회합니다."
    )
    @GetMapping("/dailyTypeInsight/platformAccount/{platformAccountId}")
    public ServiceResult<TypeInsightResponse> getTypeInsightsByPlatformAccount(
        @PathVariable Long platformAccountId,
        @RequestParam("contentType") String contentTypeId
    ) {
        return dailyTypeInsightService.getTypeInsightsByPlatformAccount(platformAccountId, contentTypeId);
    }
}
