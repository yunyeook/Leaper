package com.ssafy.leaper.domain.type.controller;

import com.ssafy.leaper.domain.type.dto.response.PlatformTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ContentTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.ProviderTypeResponse;
import com.ssafy.leaper.domain.type.dto.response.CategoryTypeResponse;
import com.ssafy.leaper.domain.type.service.TypeService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/type")
@RequiredArgsConstructor
@Tag(name = "Type", description = "타입 관리 API")
public class TypeController implements BaseController {

    private final TypeService typeService;

    @GetMapping("/platform")
    @Operation(summary = "플랫폼 타입 전체 조회", description = "모든 플랫폼 타입을 조회합니다.")
    public ResponseEntity<ApiResponse<List<PlatformTypeResponse>>> getAllPlatformTypes() {
        return handle(typeService.getAllPlatformTypes());
    }

    @GetMapping("/content")
    @Operation(summary = "콘텐츠 타입 전체 조회", description = "모든 콘텐츠 타입을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ContentTypeResponse>>> getAllContentTypes() {
        return handle(typeService.getAllContentTypes());
    }

    @GetMapping("/provider")
    @Operation(summary = "프로바이더 타입 전체 조회", description = "모든 프로바이더 타입을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ProviderTypeResponse>>> getAllProviderTypes() {
        return handle(typeService.getAllProviderTypes());
    }

    @GetMapping("/category")
    @Operation(summary = "카테고리 타입 전체 조회", description = "모든 카테고리 타입을 조회합니다.")
    public ResponseEntity<ApiResponse<List<CategoryTypeResponse>>> getAllCategoryTypes() {
        return handle(typeService.getAllCategoryTypes());
    }
}