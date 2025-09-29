package com.ssafy.spark.domain.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 전체 인사이트 분석 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllInsightsBenchmarkResult {

    private long totalExecutionTimeMs;
    private int totalInsightsCount;
    private List<InsightBenchmarkResult> insights;
    private double averageTimePerInsight;
    private String summary;

    public void calculateAverage() {
        if (insights != null && !insights.isEmpty()) {
            this.averageTimePerInsight = totalExecutionTimeMs / (double) insights.size();
        }
    }
}
