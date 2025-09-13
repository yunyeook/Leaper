package com.ssafy.leaper.domain.content.controller;

import com.ssafy.leaper.domain.content.dto.response.ContentDetailResponse;
import com.ssafy.leaper.domain.content.dto.response.SimilarContentListResponse;
import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentListResponse;
import com.ssafy.leaper.domain.content.service.ContentService;
import com.ssafy.leaper.global.common.controller.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "콘텐츠 관리 API")
public class ContentController implements BaseController {

  private final ContentService contentService;

  @Operation(
      summary = "플랫폼 계정별 콘텐츠 목록 조회",
      description = "특정 플랫폼 계정의 모든 콘텐츠를 조회합니다."
  )
  @GetMapping("/platformAccount/{platformAccountId}")
  public ResponseEntity<ApiResponse<ContentListResponse>> getContentsByPlatformAccount(
      @PathVariable Long platformAccountId) {
   return handle(contentService.getContentsByPlatformAccountId(platformAccountId));
  }
  @Operation(
      summary = "콘텐츠 상세 정보 조회",
      description = "콘텐츠 ID로 특정 콘텐츠의 상세 정보를 조회합니다. 50위 이내일 경우 랭킹 정보도 포함됩니다."
  )
  @GetMapping("/{contentId}")
  public ResponseEntity<ApiResponse<ContentDetailResponse>> getContent(
      @PathVariable Long contentId) {
    return handle(contentService.getContentById(contentId));
  }

  @Operation(
      summary = "유사 콘텐츠 조회",
      description = """
            특정 콘텐츠 ID를 기준으로 같은 플랫폼·카테고리·컨텐츠타입 내 유사한 콘텐츠를 조회합니다.
            정렬 기준:
            1) 태그 겹치는 개수
            2) 영상 길이 ±5분
            3) 인플루언서 팔로워 수 (많은 순)
            """
  )
  @GetMapping("/{contentId}/similar")
  public ResponseEntity<ApiResponse<SimilarContentListResponse>> getSimilarContents(
      @PathVariable Long contentId
  ) {
    return handle(contentService.getSimilarContents(contentId));
  }

}