package com.ssafy.spark.domain.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 인사이트 분석 벤치마크 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightBenchmarkResult {

    private String insightName;
    private long executionTimeMs;
    private long recordsProcessed;
    private long recordsSaved;
    private double recordsPerSecond;
    private String status;
    private String errorMessage;

    public void calculateThroughput() {
        if (executionTimeMs > 0) {
            this.recordsPerSecond = (recordsProcessed * 1000.0) / executionTimeMs;
        }
    }
}
