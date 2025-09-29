package com.ssafy.spark.domain.insight.controller;

import com.ssafy.spark.domain.insight.service.DummyDataGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 더미 데이터 생성 API
 */
@Slf4j
@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
@Tag(name = "Dummy Data Generator", description = "테스트용 더미 데이터 생성 API")
public class DummyDataController {

    private final DummyDataGeneratorService dummyDataService;

    /**
     * 전체 더미 데이터셋 생성 (추천)
     */
    @PostMapping("/generate-full")
    @Operation(summary = "전체 더미 데이터셋 생성", description = "콘텐츠 + 계정 데이터를 생성하고 JSON과 Parquet 형식으로 S3에 저장합니다.")
    public ResponseEntity<Map<String, Object>> generateFullDataset(
            @Parameter(description = "플랫폼 타입", example = "youtube") @RequestParam String platformType,

            @Parameter(description = "대상 날짜 (yyyy-MM-dd)", example = "2025-01-20") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,

            @Parameter(description = "생성할 콘텐츠 수", example = "10000") @RequestParam(defaultValue = "10000") int contentCount,

            @Parameter(description = "생성할 계정 수", example = "1000") @RequestParam(defaultValue = "1000") int accountCount) {
        log.info("=== 전체 더미 데이터셋 생성 요청 ===");
        log.info("Platform: {}, Date: {}, Content: {}, Account: {}",
                platformType, targetDate, contentCount, accountCount);

        long startTime = System.currentTimeMillis();

        dummyDataService.generateFullDataset(
                platformType.toLowerCase(),
                targetDate,
                contentCount,
                accountCount);

        long endTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("platformType", platformType.toLowerCase());
        response.put("targetDate", targetDate.toString());
        response.put("contentCount", contentCount);
        response.put("accountCount", accountCount);
        response.put("executionTimeMs", endTime - startTime);
        response.put("message", String.format(
                "더미 데이터 생성 완료: 콘텐츠 %d건, 계정 %d건 (%.2f초 소요)",
                contentCount, accountCount, (endTime - startTime) / 1000.0));

        return ResponseEntity.ok(response);
    }

    /**
     * DB 기본 데이터 설정
     */
    @PostMapping("/setup-database")
    @Operation(summary = "DB 기본 데이터 설정", description = "platform_type, category_type 등 기본 데이터를 DB에 저장합니다.")
    public ResponseEntity<Map<String, Object>> setupDatabase(
            @Parameter(description = "플랫폼 타입", example = "youtube") @RequestParam String platformType) {
        log.info("=== DB 기본 데이터 설정 요청: {} ===", platformType);

        dummyDataService.setupDatabase(platformType.toLowerCase());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("platformType", platformType.toLowerCase());
        response.put("message", "DB 기본 데이터 설정 완료");

        return ResponseEntity.ok(response);
    }

    /**
     * 사용 가이드
     */
    @GetMapping("/guide")
    @Operation(summary = "더미 데이터 생성 가이드", description = "더미 데이터 생성 API 사용 방법을 제공합니다.")
    public ResponseEntity<Map<String, Object>> getGuide() {
        Map<String, Object> guide = new HashMap<>();

        guide.put("title", "더미 데이터 생성 가이드");
        guide.put("description", "성능 벤치마크 테스트를 위한 더미 데이터를 생성합니다.");

        Map<String, String> steps = new HashMap<>();
        steps.put("1. DB 설정", "POST /api/dummy/setup-database?platformType=youtube");
        steps.put("2. 전체 데이터 생성",
                "POST /api/dummy/generate-full?platformType=youtube&targetDate=2025-01-20&contentCount=10000&accountCount=1000");
        steps.put("3. 성능 측정", "POST /api/benchmark/overall-performance");
        guide.put("steps", steps);

        Map<String, String> examples = new HashMap<>();
        examples.put("소량 테스트 (빠름)",
                "contentCount=1000&accountCount=100");
        examples.put("중량 테스트 (보통)",
                "contentCount=10000&accountCount=1000");
        examples.put("대량 테스트 (느림)",
                "contentCount=100000&accountCount=10000");
        guide.put("examples", examples);

        Map<String, String> warnings = new HashMap<>();
        warnings.put("S3 비용", "대량 데이터 생성 시 S3 저장 비용 발생");
        warnings.put("처리 시간", "10만건 생성 시 수 분 소요");
        warnings.put("메모리", "Spark 변환 시 충분한 메모리 필요");
        guide.put("warnings", warnings);

        return ResponseEntity.ok(guide);
    }
}
