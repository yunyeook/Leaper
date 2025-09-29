package com.ssafy.spark.domain.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 성능 측정 결과를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResult {

    private String testName; // 테스트 이름
    private long executionTimeMs; // 실행 시간 (밀리초)
    private long recordCount; // 처리된 레코드 수
    private double recordsPerSecond; // 초당 처리량
    private String dataFormat; // 데이터 포맷 (JSON/Parquet)
    private String insertMethod; // 저장 방식 (Individual/Batch)
    private long memoryUsedMB; // 사용된 메모리 (MB)

    /**
     * 초당 처리량 계산
     */
    public void calculateThroughput() {
        if (executionTimeMs > 0) {
            this.recordsPerSecond = (recordCount * 1000.0) / executionTimeMs;
        }
    }

    /**
     * 성능 향상률 계산 (다른 결과 대비)
     */
    public double getImprovementRate(BenchmarkResult baseline) {
        if (baseline.getExecutionTimeMs() == 0)
            return 0.0;
        return ((baseline.getExecutionTimeMs() - this.executionTimeMs) * 100.0) / baseline.getExecutionTimeMs();
    }
}
