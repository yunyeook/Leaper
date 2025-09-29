package com.ssafy.spark.domain.insight.controller;

import com.ssafy.spark.domain.insight.dto.AllInsightsBenchmarkResult;
import com.ssafy.spark.domain.insight.dto.PerformanceComparisonResult;
import com.ssafy.spark.domain.insight.service.InsightAnalysisBenchmarkService;
import com.ssafy.spark.domain.insight.service.PerformanceBenchmarkService;
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
 * 성능 벤치마크 측정 API
 */
@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
@Tag(name = "Performance Benchmark", description = "성능 측정 API")
public class PerformanceBenchmarkController {

        private final PerformanceBenchmarkService benchmarkService;
        private final InsightAnalysisBenchmarkService insightBenchmarkService;

        /**
         * 데이터 읽기 성능 비교 (JSON vs Parquet)
         */
        @PostMapping("/read-performance")
        @Operation(summary = "데이터 읽기 성능 비교", description = "JSON vs Parquet 읽기 성능을 비교합니다.")
        public ResponseEntity<PerformanceComparisonResult> compareReadPerformance(
                        @Parameter(description = "플랫폼 타입 (YOUTUBE, INSTAGRAM 등)", example = "youtube") @RequestParam String platformType,

                        @Parameter(description = "대상 날짜 (yyyy-MM-dd)", example = "2025-01-20") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                log.info("=== 데이터 읽기 성능 비교 요청: platform={}, date={} ===", platformType, targetDate);

                PerformanceComparisonResult result = benchmarkService.compareReadPerformance(
                                platformType.toLowerCase(),
                                targetDate);

                return ResponseEntity.ok(result);
        }

        /**
         * DB 저장 성능 비교 (개별 INSERT vs Batch INSERT)
         */
        @PostMapping("/insert-performance")
        @Operation(summary = "DB 저장 성능 비교", description = "개별 INSERT vs Batch INSERT 성능을 비교합니다.")
        public ResponseEntity<PerformanceComparisonResult> compareInsertPerformance(
                        @Parameter(description = "테스트 레코드 수", example = "1000") @RequestParam(defaultValue = "1000") int recordCount) {
                log.info("=== DB 저장 성능 비교 요청: recordCount={} ===", recordCount);

                PerformanceComparisonResult result = benchmarkService.compareInsertPerformance(recordCount);

                return ResponseEntity.ok(result);
        }

        /**
         * 전체 성능 비교 (JSON + 개별 INSERT vs Parquet + Batch INSERT)
         */
        @PostMapping("/overall-performance")
        @Operation(summary = "전체 성능 비교", description = "JSON + 개별 INSERT vs Parquet + Batch INSERT 전체 처리 성능을 비교합니다.")
        public ResponseEntity<PerformanceComparisonResult> compareOverallPerformance(
                        @Parameter(description = "플랫폼 타입", example = "youtube") @RequestParam String platformType,

                        @Parameter(description = "대상 날짜 (yyyy-MM-dd)", example = "2025-01-20") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,

                        @Parameter(description = "DB 저장 테스트 레코드 수", example = "1000") @RequestParam(defaultValue = "1000") int insertRecordCount) {
                log.info("=== 전체 성능 비교 요청: platform={}, date={}, insertRecordCount={} ===",
                                platformType, targetDate, insertRecordCount);

                PerformanceComparisonResult result = benchmarkService.compareOverallPerformance(
                                platformType.toLowerCase(),
                                targetDate,
                                insertRecordCount);

                return ResponseEntity.ok(result);
        }

        /**
         * 인사이트 분석 성능 측정 (순차 실행)
         */
        @PostMapping("/insights-sequential")
        @Operation(summary = "인사이트 분석 성능 측정 (순차)", description = "8개 인사이트 분석을 순차적으로 실행하고 각각의 성능을 측정합니다.")
        public ResponseEntity<AllInsightsBenchmarkResult> measureInsightsSequential(
                        @Parameter(description = "플랫폼 타입", example = "youtube") @RequestParam String platformType,
                        @Parameter(description = "대상 날짜 (yyyy-MM-dd)", example = "2025-12-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                log.info("=== 인사이트 순차 분석 성능 측정 요청: platform={}, date={} ===", platformType, targetDate);

                AllInsightsBenchmarkResult result = insightBenchmarkService.measureAllInsightsSequential(
                                platformType.toLowerCase(),
                                targetDate);

                return ResponseEntity.ok(result);
        }

        /**
         * 인사이트 분석 성능 측정 (병렬 실행)
         */
        @PostMapping("/insights-parallel")
        @Operation(summary = "인사이트 분석 성능 측정 (병렬)", description = "8개 인사이트 분석을 병렬로 실행하고 각각의 성능을 측정합니다.")
        public ResponseEntity<AllInsightsBenchmarkResult> measureInsightsParallel(
                        @Parameter(description = "플랫폼 타입", example = "youtube") @RequestParam String platformType,
                        @Parameter(description = "대상 날짜 (yyyy-MM-dd)", example = "2025-12-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                log.info("=== 인사이트 병렬 분석 성능 측정 요청: platform={}, date={} ===", platformType, targetDate);

                AllInsightsBenchmarkResult result = insightBenchmarkService.measureAllInsightsParallel(
                                platformType.toLowerCase(),
                                targetDate);

                return ResponseEntity.ok(result);
        }

        /**
         * 성능 측정 가이드
         */
        @GetMapping("/guide")
        @Operation(summary = "성능 측정 가이드", description = "벤치마크 API 사용 방법을 제공합니다.")
        public ResponseEntity<Map<String, Object>> getGuide() {
                Map<String, Object> guide = new HashMap<>();

                guide.put("title", "성능 벤치마크 측정 가이드");
                guide.put("description", "JSON vs Parquet, 개별 INSERT vs Batch INSERT, 인사이트 분석 성능을 비교합니다.");

                Map<String, String> endpoints = new HashMap<>();
                endpoints.put("POST /api/benchmark/read-performance", "데이터 읽기 성능 비교");
                endpoints.put("POST /api/benchmark/insert-performance", "DB 저장 성능 비교");
                endpoints.put("POST /api/benchmark/overall-performance", "전체 성능 비교 (읽기+저장)");
                endpoints.put("POST /api/benchmark/insights-sequential", "인사이트 분석 성능 (순차)");
                endpoints.put("POST /api/benchmark/insights-parallel", "인사이트 분석 성능 (병렬)");
                guide.put("endpoints", endpoints);

                return ResponseEntity.ok(guide);
        }
}
