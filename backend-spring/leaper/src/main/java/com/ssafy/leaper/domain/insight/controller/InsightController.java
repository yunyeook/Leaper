package com.ssafy.leaper.domain.insight.controller;

import com.ssafy.leaper.domain.insight.dto.response.contentCommentInsight.ContentCommentInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight.AccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight.DailyAccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight.InfluencerViewsResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight.DailyTrendingContentResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight.DailyTrendingInfluencerResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight.TypeInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight.DailyMyPopularContentResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight.DailyPopularContentResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight.DailyPopularInfluencerResponse;
import com.ssafy.leaper.domain.insight.dto.response.trend.KeywordTrendResponse;
import com.ssafy.leaper.domain.insight.service.ContentCommentInsightService;
import com.ssafy.leaper.domain.insight.service.DailyAccountInsightService;
import com.ssafy.leaper.domain.insight.service.DailyPopularInsightService;
import com.ssafy.leaper.domain.insight.service.DailyTrendingInsightService;
import com.ssafy.leaper.domain.insight.service.DailyTypeInsightService;
import com.ssafy.leaper.domain.insight.service.KeywordTrendService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
@Tag(name = "Insight API", description = "인플루언서 인사이트 관련 API")
public class InsightController implements BaseController {

    private final DailyAccountInsightService dailyAccountInsightService;
    private final DailyTypeInsightService dailyTypeInsightService;
    private final DailyPopularInsightService dailyPopularInsightService;
    private final DailyTrendingInsightService dailyTrendingInsightService;
    private final ContentCommentInsightService contentCommentInsightService;
    private final KeywordTrendService keywordTrendService;

    @Operation(
        summary = "인플루언서 계정 인사이트 전체 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 현재 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}/today")
    public ResponseEntity<ApiResponse<List<DailyAccountInsightResponse>>> getAccountInsightsToday(
        @PathVariable Integer influencerId
    ) {
        return handle(dailyAccountInsightService.getAccountInsightsToday(influencerId));
    }

    @Operation(
        summary = "인플루언서 계정 인사이트 전체 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}")
    public ResponseEntity<ApiResponse<AccountInsightResponse>> getAccountInsights(
        @PathVariable Integer influencerId
    ) {
        return handle(dailyAccountInsightService.getAccountInsights(influencerId));
    }

    @Operation(
        summary = "인플루언서 계정 조회수만 조회",
        description = "특정 인플루언서의 모든 플랫폼 계정에 대해 일별 및 월별 조회수를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/influencer/{influencerId}/view")
    public ResponseEntity<ApiResponse<InfluencerViewsResponse>> getInfluencerViews(
        @PathVariable Integer influencerId
    ) {
        return handle(dailyAccountInsightService.getInfluencerViews(influencerId));
    }

    @Operation(
        summary = "플랫폼 계정 인사이트 전체 조회",
        description = "특정 플랫폼 계정에 대해 현재 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/platformAccount/{platformAccountId}/today")
    public ResponseEntity<ApiResponse<DailyAccountInsightResponse>> getPlatformAccountInsightsToday(
        @PathVariable Integer platformAccountId
    ) {
        return handle(dailyAccountInsightService.getPlatformAccountInsightsToday(platformAccountId));
    }
    @Operation(
        summary = "플랫폼 계정 인사이트 전체 조회",
        description = "특정 플랫폼 계정에 대해 일별 및 월별 누적 인사이트(조회수, 팔로워, 좋아요, 댓글 등)를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/platformAccount/{platformAccountId}")
    public ResponseEntity<ApiResponse<AccountInsightResponse>> getPlatformAccountInsights(
        @PathVariable Integer platformAccountId
    ) {
        return handle(dailyAccountInsightService.getPlatformAccountInsights(platformAccountId));
    }

    @Operation(
        summary = "플랫폼 계정 조회수만 조회",
        description = "특정 플랫폼 계정에 대해 일별 및 월별 조회수를 조회합니다."
    )
    @GetMapping("/dailyAccountInsight/platformAccount/{platformAccountId}/view")
    public ResponseEntity<ApiResponse<InfluencerViewsResponse>> getPlatformAccountViews(
        @PathVariable Integer platformAccountId
    ) {
        return handle(dailyAccountInsightService.getPlatformAccountViews(platformAccountId));
    }

    @Operation(
        summary = "인플루언서 타입 인사이트 조회",
        description = "특정 인플루언서의 계정별로 특정 ContentType(영상, 쇼츠, 포스트 등)의 일별 및 월별 인사이트를 조회합니다."
    )
    @GetMapping("/dailyTypeInsight/influencer/{influencerId}")
    public ResponseEntity<ApiResponse<TypeInsightResponse>> getTypeInsightsByInfluencer(
        @PathVariable Long influencerId,
        @RequestParam("contentType") String contentTypeId
    ) {
        return handle(dailyTypeInsightService.getTypeInsightsByInfluencer(influencerId, contentTypeId));
    }

    @Operation(
        summary = "플랫폼 계정 타입 인사이트 조회",
        description = "특정 플랫폼 계정에 대해 특정 ContentType의 일별 및 월별 인사이트를 조회합니다."
    )
    @GetMapping("/dailyTypeInsight/platformAccount/{platformAccountId}")
    public ResponseEntity<ApiResponse<TypeInsightResponse>> getTypeInsightsByPlatformAccount(
        @PathVariable Long platformAccountId,
        @RequestParam("contentType") String contentTypeId
    ) {
        return handle(dailyTypeInsightService.getTypeInsightsByPlatformAccount(platformAccountId, contentTypeId));
    }

    @Operation(
        summary = "일별 인기 콘텐츠 조회",
        description = "특정 플랫폼, 카테고리에서 가장 인기 있는 콘텐츠 10개를 조회합니다"
    )
    @GetMapping("/dailyPopularContent/content")
    public ResponseEntity<ApiResponse<DailyPopularContentResponse>> getDailyPopularContents(
        @RequestParam String platformType,
        @RequestParam Long categoryType
    ) {
        return handle(dailyPopularInsightService.getPopularContents(platformType, categoryType));
    }

    @Operation(
        summary = "일별 인기 인플루언서 조회",
        description = "특정 플랫폼, 카테고리에서 가장 인기 있는 인플루언서 10명을 조회합니다."
    )
    @GetMapping("/dailyPopularInfluencer/influencer")
    public ResponseEntity<ApiResponse<DailyPopularInfluencerResponse>> getPopularInfluencers(
        @RequestParam String platformType,
        @RequestParam Long categoryType
    ) {
        return handle(dailyPopularInsightService.getPopularInfluencers(platformType, categoryType));
    }

    @Operation(
        summary = "특정 계정의 인기 콘텐츠 조회",
        description = "해당 플랫폼 계정 ID 기준으로 최신 날짜 인기 콘텐츠 TOP3을 조회합니다."
    )
    @GetMapping("/dailyMyPopularContent/platformAccount/{platformAccountId}")
    public ResponseEntity<ApiResponse<DailyMyPopularContentResponse>> getMyPopularContents(
        @PathVariable Long platformAccountId
    ) {
        return handle(dailyPopularInsightService.getMyPopularContents(platformAccountId));
    }

    @Operation(
        summary = "일별 급상승 콘텐츠 조회",
        description = "특정 플랫폼, 카테고리에서 급상승하는 콘텐츠 10개를 조회합니다"
    )
    @GetMapping("/dailyTrendingContent/content")
    public ResponseEntity<ApiResponse<DailyTrendingContentResponse>> getDailyTrendingContents(
        @RequestParam String platformType,
        @RequestParam Long categoryType
    ) {
        return handle(dailyTrendingInsightService.getTrendingContents(platformType, categoryType));
    }

    @Operation(
        summary = "일별 급상승 인플루언서 조회",
        description = "특정 플랫폼, 카테고리에서 급상승하는 인플루언서 10명을 조회합니다."
    )
    @GetMapping("/dailyTrendingInfluencer/influencer")
    public ResponseEntity<ApiResponse<DailyTrendingInfluencerResponse>> getTrendingInfluencers(
        @RequestParam String platformType,
        @RequestParam Long categoryType
    ) {
        return handle(dailyTrendingInsightService.getTrendingInfluencers(platformType, categoryType));
    }

    @Operation(
        summary = "콘텐츠 댓글 인사이트 조회",
        description = "특정 콘텐츠의 댓글 분석 결과(긍정/부정 댓글, 키워드, 요약 등)를 조회합니다."
    )
    @GetMapping("/commentInsight/content/{contentId}")
    public ResponseEntity<ApiResponse<ContentCommentInsightResponse>> getContentCommentInsight(
        @PathVariable Long contentId
    ) {
        return handle(contentCommentInsightService.getContentCommentInsight(contentId));
    }

    @Operation(
        summary = "인기 키워드 조회",
        description = "특정 플랫폼의 인기 키워드를 조회합니다."
    )
    @GetMapping("/trend/platformType/{platformTypeId}")
    public ResponseEntity<ApiResponse<KeywordTrendResponse>> getDailyKeywordTrend(
        @PathVariable String platformTypeId

        ) {
        return handle(keywordTrendService.getDailyKeywordTrendLatestSnapshot(platformTypeId));
    }

    @Operation(
        summary = "구글 트렌드 키워드 조회",
        description = "구글 트렌드의 인기 키워드를 조회합니다."
    )
    @GetMapping("/trend/google")
    public ResponseEntity<ApiResponse<KeywordTrendResponse>> getGoogleKeywordTrend(

    ) {
        return handle(keywordTrendService.getGoogleKeywordTrendLatestSnapshot());
    }


}
