package com.ssafy.spark.domain.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 성능 비교 결과를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceComparisonResult {

    private String comparisonType; // 비교 유형 (Read/Insert/Overall)
    private BenchmarkResult baseline; // 기준 성능 (JSON/개별INSERT)
    private BenchmarkResult optimized; // 최적화 성능 (Parquet/Batch)
    private double improvementPercent; // 성능 향상률 (%)
    private double speedupFactor; // 속도 향상 배수
    private String summary; // 요약 설명

    /**
     * 성능 향상률 계산
     */
    public void calculateImprovement() {
        if (baseline != null && optimized != null) {
            this.improvementPercent = optimized.getImprovementRate(baseline);
            if (optimized.getExecutionTimeMs() > 0) {
                this.speedupFactor = (double) baseline.getExecutionTimeMs() / optimized.getExecutionTimeMs();
            }
        }
    }
}
