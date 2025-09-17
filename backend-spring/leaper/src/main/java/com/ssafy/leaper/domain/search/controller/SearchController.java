package com.ssafy.leaper.domain.search.controller;

import com.ssafy.leaper.domain.search.dto.request.InfluencerSearchRequest;
import com.ssafy.leaper.domain.search.dto.response.InfluencerSearchResponse;
import com.ssafy.leaper.domain.search.service.SearchService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "검색 API")
public class SearchController implements BaseController {

    private final SearchService searchService;

    @Operation(
        summary = "인플루언서 검색",
        description = """
            인플루언서를 다양한 조건으로 검색합니다.
            - keyword: 닉네임 또는 bio에서 검색
            - platform: 플랫폼 유형 필터
            - category: 카테고리 필터
            - contentType: 콘텐츠 유형 필터
            - minFollowers: 최소 팔로워 수
            - maxFollowers: 최대 팔로워 수
            """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<InfluencerSearchResponse>> searchInfluencer(
            Authentication authentication,
        @Valid @RequestBody InfluencerSearchRequest request
    ) {
        return handle(searchService.searchInfluencers(request));
    }
}