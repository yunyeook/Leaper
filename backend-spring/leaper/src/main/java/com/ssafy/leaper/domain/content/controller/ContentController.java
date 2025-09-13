package com.ssafy.leaper.domain.content.controller;

import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.domain.content.dto.ContentListResponse;
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
}