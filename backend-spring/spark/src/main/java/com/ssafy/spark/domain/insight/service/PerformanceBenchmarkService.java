package com.ssafy.spark.domain.insight.service;

import com.ssafy.spark.domain.insight.dto.BenchmarkResult;
import com.ssafy.spark.domain.insight.dto.PerformanceComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 성능 측정 벤치마크 서비스
 * JSON vs Parquet, 개별 INSERT vs Batch INSERT 성능 비교
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceBenchmarkService extends SparkBaseService {

    /**
     * 데이터 읽기 성능 비교 (JSON vs Parquet)
     */
    public PerformanceComparisonResult compareReadPerformance(String platformType, LocalDate targetDate) {
        log.info("=== 데이터 읽기 성능 비교 시작 ===");

        // JSON 읽기 성능 측정
        BenchmarkResult jsonResult = measureJsonReadPerformance(platformType, targetDate);

        // Parquet 읽기 성능 측정
        BenchmarkResult parquetResult = measureParquetReadPerformance(platformType, targetDate);

        // 비교 결과 생성
        PerformanceComparisonResult comparison = PerformanceComparisonResult.builder()
                .comparisonType("Data Read Performance")
                .baseline(jsonResult)
                .optimized(parquetResult)
                .build();

        comparison.calculateImprovement();
        comparison.setSummary(String.format(
                "Parquet이 JSON보다 %.2f배 빠르며, %.1f%% 성능 향상",
                comparison.getSpeedupFactor(),
                comparison.getImprovementPercent()));

        log.info("=== 데이터 읽기 성능 비교 완료 ===");
        log.info("JSON: {}ms, Parquet: {}ms, 향상률: {:.1f}%",
                jsonResult.getExecutionTimeMs(),
                parquetResult.getExecutionTimeMs(),
                comparison.getImprovementPercent());

        return comparison;
    }

    /**
     * JSON 읽기 성능 측정
     */
    private BenchmarkResult measureJsonReadPerformance(String platformType, LocalDate targetDate) {
        log.info(">>> JSON 읽기 성능 측정 시작");

        String dateFolder = targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String jsonPath = String.format("s3a://%s/raw_data/json/%s/content/%s/*.json",
                bucketName, platformType, dateFolder);

        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        long startTime = System.currentTimeMillis();

        try {
            Dataset<Row> jsonData = sparkSession.read()
                    .option("multiline", "true")
                    .json(jsonPath);

            long count = jsonData.count(); // Action 트리거

            long endTime = System.currentTimeMillis();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

            BenchmarkResult result = BenchmarkResult.builder()
                    .testName("JSON 읽기")
                    .executionTimeMs(endTime - startTime)
                    .recordCount(count)
                    .dataFormat("JSON")
                    .memoryUsedMB((memoryAfter - memoryBefore) / (1024 * 1024))
                    .build();

            result.calculateThroughput();

            log.info(">>> JSON 읽기 완료: {}ms, {} records, {:.2f} records/sec",
                    result.getExecutionTimeMs(),
                    result.getRecordCount(),
                    result.getRecordsPerSecond());

            return result;

        } catch (Exception e) {
            log.error("JSON 읽기 성능 측정 실패", e);
            return BenchmarkResult.builder()
                    .testName("JSON 읽기 (실패)")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .recordCount(0)
                    .dataFormat("JSON")
                    .build();
        }
    }

    /**
     * Parquet 읽기 성능 측정
     */
    private BenchmarkResult measureParquetReadPerformance(String platformType, LocalDate targetDate) {
        log.info(">>> Parquet 읽기 성능 측정 시작");

        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        long startTime = System.currentTimeMillis();

        try {
            Dataset<Row> parquetData = readS3ContentDataByDate(platformType, targetDate);
            long count = parquetData.count(); // Action 트리거

            long endTime = System.currentTimeMillis();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

            BenchmarkResult result = BenchmarkResult.builder()
                    .testName("Parquet 읽기")
                    .executionTimeMs(endTime - startTime)
                    .recordCount(count)
                    .dataFormat("Parquet")
                    .memoryUsedMB((memoryAfter - memoryBefore) / (1024 * 1024))
                    .build();

            result.calculateThroughput();

            log.info(">>> Parquet 읽기 완료: {}ms, {} records, {:.2f} records/sec",
                    result.getExecutionTimeMs(),
                    result.getRecordCount(),
                    result.getRecordsPerSecond());

            return result;

        } catch (Exception e) {
            log.error("Parquet 읽기 성능 측정 실패", e);
            return BenchmarkResult.builder()
                    .testName("Parquet 읽기 (실패)")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .recordCount(0)
                    .dataFormat("Parquet")
                    .build();
        }
    }

    /**
     * DB 저장 성능 비교 (개별 INSERT vs Batch INSERT)
     */
    public PerformanceComparisonResult compareInsertPerformance(int recordCount) {
        log.info("=== DB 저장 성능 비교 시작 (레코드 수: {}) ===", recordCount);

        // 테스트 데이터 생성
        List<TestData> testDataList = generateTestData(recordCount);

        // 개별 INSERT 성능 측정
        BenchmarkResult individualResult = measureIndividualInsert(testDataList);

        // Batch INSERT 성능 측정
        BenchmarkResult batchResult = measureBatchInsert(testDataList);

        // 비교 결과 생성
        PerformanceComparisonResult comparison = PerformanceComparisonResult.builder()
                .comparisonType("DB Insert Performance")
                .baseline(individualResult)
                .optimized(batchResult)
                .build();

        comparison.calculateImprovement();
        comparison.setSummary(String.format(
                "Batch INSERT가 개별 INSERT보다 %.2f배 빠르며, %.1f%% 성능 향상",
                comparison.getSpeedupFactor(),
                comparison.getImprovementPercent()));

        log.info("=== DB 저장 성능 비교 완료 ===");
        log.info("개별: {}ms, Batch: {}ms, 향상률: {:.1f}%",
                individualResult.getExecutionTimeMs(),
                batchResult.getExecutionTimeMs(),
                comparison.getImprovementPercent());

        return comparison;
    }

    /**
     * 개별 INSERT 성능 측정
     */
    private BenchmarkResult measureIndividualInsert(List<TestData> testDataList) {
        log.info(">>> 개별 INSERT 성능 측정 시작 ({} records)", testDataList.size());

        // 테스트 테이블 생성
        createBenchmarkTable();

        long startTime = System.currentTimeMillis();

        try {
            String sql = "INSERT INTO benchmark_test (platform_type, content_id, value, created_at) VALUES (?, ?, ?, ?)";

            int insertCount = 0;
            for (TestData data : testDataList) {
                jdbcTemplate.update(sql,
                        data.platformType,
                        data.contentId,
                        data.value,
                        LocalDateTime.now());
                insertCount++;
            }

            long endTime = System.currentTimeMillis();

            BenchmarkResult result = BenchmarkResult.builder()
                    .testName("개별 INSERT")
                    .executionTimeMs(endTime - startTime)
                    .recordCount(insertCount)
                    .insertMethod("Individual")
                    .build();

            result.calculateThroughput();

            log.info(">>> 개별 INSERT 완료: {}ms, {} records, {:.2f} records/sec",
                    result.getExecutionTimeMs(),
                    result.getRecordCount(),
                    result.getRecordsPerSecond());

            // 테스트 데이터 삭제
            cleanupBenchmarkTable();

            return result;

        } catch (Exception e) {
            log.error("개별 INSERT 성능 측정 실패", e);
            cleanupBenchmarkTable();
            return BenchmarkResult.builder()
                    .testName("개별 INSERT (실패)")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .recordCount(0)
                    .insertMethod("Individual")
                    .build();
        }
    }

    /**
     * Batch INSERT 성능 측정
     */
    private BenchmarkResult measureBatchInsert(List<TestData> testDataList) {
        log.info(">>> Batch INSERT 성능 측정 시작 ({} records)", testDataList.size());

        // 테스트 테이블 생성
        createBenchmarkTable();

        long startTime = System.currentTimeMillis();

        try {
            String sql = "INSERT INTO benchmark_test (platform_type, content_id, value, created_at) VALUES (?, ?, ?, ?)";

            LocalDateTime now = LocalDateTime.now();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    TestData data = testDataList.get(i);
                    ps.setString(1, data.platformType);
                    ps.setInt(2, data.contentId);
                    ps.setInt(3, data.value);
                    ps.setObject(4, now);
                }

                @Override
                public int getBatchSize() {
                    return testDataList.size();
                }
            });

            long endTime = System.currentTimeMillis();

            BenchmarkResult result = BenchmarkResult.builder()
                    .testName("Batch INSERT")
                    .executionTimeMs(endTime - startTime)
                    .recordCount(testDataList.size())
                    .insertMethod("Batch")
                    .build();

            result.calculateThroughput();

            log.info(">>> Batch INSERT 완료: {}ms, {} records, {:.2f} records/sec",
                    result.getExecutionTimeMs(),
                    result.getRecordCount(),
                    result.getRecordsPerSecond());

            // 테스트 데이터 삭제
            cleanupBenchmarkTable();

            return result;

        } catch (Exception e) {
            log.error("Batch INSERT 성능 측정 실패", e);
            cleanupBenchmarkTable();
            return BenchmarkResult.builder()
                    .testName("Batch INSERT (실패)")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .recordCount(0)
                    .insertMethod("Batch")
                    .build();
        }
    }

    /**
     * 전체 성능 비교 (JSON + 개별 INSERT vs Parquet + Batch INSERT)
     */
    public PerformanceComparisonResult compareOverallPerformance(String platformType, LocalDate targetDate,
            int insertRecordCount) {
        log.info("=== 전체 성능 비교 시작 ===");

        long baselineStartTime = System.currentTimeMillis();

        // 1. JSON 읽기
        BenchmarkResult jsonRead = measureJsonReadPerformance(platformType, targetDate);

        // 2. 개별 INSERT
        List<TestData> testData = generateTestData(insertRecordCount);
        BenchmarkResult individualInsert = measureIndividualInsert(testData);

        long baselineEndTime = System.currentTimeMillis();
        long baselineTotal = baselineEndTime - baselineStartTime;

        // ===== 최적화 방식 =====

        long optimizedStartTime = System.currentTimeMillis();

        // 1. Parquet 읽기
        BenchmarkResult parquetRead = measureParquetReadPerformance(platformType, targetDate);

        // 2. Batch INSERT
        BenchmarkResult batchInsert = measureBatchInsert(testData);

        long optimizedEndTime = System.currentTimeMillis();
        long optimizedTotal = optimizedEndTime - optimizedStartTime;

        // 전체 결과 생성
        BenchmarkResult baselineOverall = BenchmarkResult.builder()
                .testName("전체 (JSON + 개별 INSERT)")
                .executionTimeMs(baselineTotal)
                .recordCount(jsonRead.getRecordCount() + individualInsert.getRecordCount())
                .dataFormat("JSON")
                .insertMethod("Individual")
                .build();
        baselineOverall.calculateThroughput();

        BenchmarkResult optimizedOverall = BenchmarkResult.builder()
                .testName("전체 (Parquet + Batch INSERT)")
                .executionTimeMs(optimizedTotal)
                .recordCount(parquetRead.getRecordCount() + batchInsert.getRecordCount())
                .dataFormat("Parquet")
                .insertMethod("Batch")
                .build();
        optimizedOverall.calculateThroughput();

        PerformanceComparisonResult comparison = PerformanceComparisonResult.builder()
                .comparisonType("Overall Performance")
                .baseline(baselineOverall)
                .optimized(optimizedOverall)
                .build();

        comparison.calculateImprovement();
        comparison.setSummary(String.format(
                "최적화 후 전체 처리가 %.2f배 빠르며, %.1f%% 성능 향상",
                comparison.getSpeedupFactor(),
                comparison.getImprovementPercent()));

        log.info("=== 전체 성능 비교 완료 ===");
        log.info("기존: {}ms, 최적화: {}ms, 향상률: {:.1f}%",
                baselineTotal, optimizedTotal, comparison.getImprovementPercent());

        return comparison;
    }

    /**
     * 테스트 데이터 생성
     */
    private List<TestData> generateTestData(int count) {
        List<TestData> dataList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dataList.add(new TestData(
                    "YOUTUBE",
                    i + 1,
                    (int) (Math.random() * 10000)));
        }
        return dataList;
    }

    /**
     * 벤치마크 테스트 테이블 생성
     */
    private void createBenchmarkTable() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS benchmark_test");
            jdbcTemplate.execute(
                    "CREATE TABLE benchmark_test (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "platform_type VARCHAR(50), " +
                            "content_id INT, " +
                            "value INT, " +
                            "created_at DATETIME)");
        } catch (Exception e) {
            log.warn("벤치마크 테이블 생성 실패 (이미 존재할 수 있음)", e);
        }
    }

    /**
     * 벤치마크 테스트 데이터 삭제
     */
    private void cleanupBenchmarkTable() {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE benchmark_test");
        } catch (Exception e) {
            log.warn("벤치마크 테이블 정리 실패", e);
        }
    }

    /**
     * 테스트 데이터 내부 클래스
     */
    private static class TestData {
        String platformType;
        int contentId;
        int value;

        TestData(String platformType, int contentId, int value) {
            this.platformType = platformType;
            this.contentId = contentId;
            this.value = value;
        }
    }
}
