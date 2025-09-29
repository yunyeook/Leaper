package com.ssafy.spark.domain.insight.service;

import com.ssafy.spark.domain.insight.dto.AllInsightsBenchmarkResult;
import com.ssafy.spark.domain.insight.dto.InsightBenchmarkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 인사이트 분석 성능 벤치마크 서비스
 * 8개 인사이트 분석의 성능을 측정하고 비교합니다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightAnalysisBenchmarkService extends SparkBaseService {

    private final AccountInsightService accountInsightService;
    private final SparkTypeInsightService sparkTypeInsightService;
    private final SparkPopularContentService sparkPopularContentService;
    private final SparkPopularInfluencerService sparkPopularInfluencerService;
    private final AccountPopularContentService accountPopularContentService;
    private final SparkTrendingInfluencerService sparkTrendingInfluencerService;
    private final SparkTrendingContentService sparkTrendingContentService;
    private final SparkKeywordTrendService sparkKeywordTrendService;

    /**
     * 전체 인사이트 분석 성능 측정 (순차 실행)
     */
    public AllInsightsBenchmarkResult measureAllInsightsSequential(String platformType, LocalDate targetDate) {
        log.info("=== 전체 인사이트 순차 분석 시작 ===");
        log.info("Platform: {}, Date: {}", platformType, targetDate);

        long totalStartTime = System.currentTimeMillis();
        List<InsightBenchmarkResult> results = new ArrayList<>();

        try {
            // 데이터 로드 및 캐싱
            Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate).cache();
            Dataset<Row> accountData = readS3AccountData(platformType, targetDate).cache();

            long contentCount = contentData.count();
            long accountCount = accountData.count();
            log.info("데이터 로드 완료 - Content: {} rows, Account: {} rows", contentCount, accountCount);

            // 1. 계정별 인사이트 (daily_account_insight, snapshot_date)
            results.add(measureSingleInsight("accountInsight", () -> {
                accountInsightService.generateDailyAccountInsight(platformType, targetDate, contentData, accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_account_insight WHERE snapshot_date = ?",
                        Long.class, targetDate);
            }, contentCount + accountCount));

            // 2. 타입별 인사이트 (daily_type_insight, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("typeInsight", () -> {
                sparkTypeInsightService.generateDailyTypeInsight(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_type_insight WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount));

            // 3. 인기 콘텐츠 (daily_popular_content, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("popularContent", () -> {
                sparkPopularContentService.generateDailyPopularContent(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_popular_content WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount + accountCount));

            // 4. 인기 인플루언서 (daily_popular_influencer, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("popularInfluencer", () -> {
                sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_popular_influencer WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, accountCount));

            // 5. 계정별 인기 콘텐츠 (daily_my_popular_content, snapshot_date)
            results.add(measureSingleInsight("accountPopularContent", () -> {
                accountPopularContentService.generateAccountPopularContent(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_my_popular_content WHERE snapshot_date = ?",
                        Long.class, targetDate);
            }, contentCount));

            // 6. 트렌딩 인플루언서 (daily_trending_influencer, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("trendingInfluencer", () -> {
                sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType, targetDate, accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_trending_influencer WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, accountCount));

            // 7. 트렌딩 콘텐츠 (daily_trending_content, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("trendingContent", () -> {
                sparkTrendingContentService.generateDailyTrendingContent(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_trending_content WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount + accountCount));

            // 8. 키워드 트렌드 (daily_keyword_trend, platform_type_id, snapshot_date)
            results.add(measureSingleInsight("keywordTrend", () -> {
                sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_keyword_trend WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount));

            // 캐시 해제
            contentData.unpersist();
            accountData.unpersist();

            long totalEndTime = System.currentTimeMillis();

            AllInsightsBenchmarkResult finalResult = AllInsightsBenchmarkResult.builder()
                    .totalExecutionTimeMs(totalEndTime - totalStartTime)
                    .totalInsightsCount(results.size())
                    .insights(results)
                    .build();

            finalResult.calculateAverage();
            finalResult.setSummary(String.format(
                    "%d개 인사이트 순차 분석 완료, 평균 %.2f초/인사이트",
                    results.size(),
                    finalResult.getAverageTimePerInsight() / 1000.0));

            log.info("=== 전체 인사이트 순차 분석 완료: {}ms ===", finalResult.getTotalExecutionTimeMs());

            return finalResult;

        } catch (Exception e) {
            log.error("인사이트 분석 측정 실패", e);
            throw new RuntimeException("인사이트 분석 측정 실패", e);
        }
    }

    /**
     * 전체 인사이트 분석 성능 측정 (병렬 실행)
     */
    public AllInsightsBenchmarkResult measureAllInsightsParallel(String platformType, LocalDate targetDate) {
        log.info("=== 전체 인사이트 병렬 분석 시작 ===");
        log.info("Platform: {}, Date: {}", platformType, targetDate);

        long totalStartTime = System.currentTimeMillis();

        try {
            // 데이터 로드 및 캐싱
            Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate).cache();
            Dataset<Row> accountData = readS3AccountData(platformType, targetDate).cache();

            long contentCount = contentData.count();
            long accountCount = accountData.count();
            log.info("데이터 로드 완료 - Content: {} rows, Account: {} rows", contentCount, accountCount);

            // 모든 인사이트를 병렬로 실행
            List<CompletableFuture<InsightBenchmarkResult>> futures = new ArrayList<>();

            // 1. 계정별 인사이트
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("accountInsight", () -> {
                accountInsightService.generateDailyAccountInsight(platformType, targetDate, contentData, accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_account_insight WHERE snapshot_date = ?",
                        Long.class, targetDate);
            }, contentCount + accountCount)));

            // 2. 타입별 인사이트
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("typeInsight", () -> {
                sparkTypeInsightService.generateDailyTypeInsight(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_type_insight WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount)));

            // 3. 인기 콘텐츠
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("popularContent", () -> {
                sparkPopularContentService.generateDailyPopularContent(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_popular_content WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount + accountCount)));

            // 4. 인기 인플루언서
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("popularInfluencer", () -> {
                sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_popular_influencer WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, accountCount)));

            // 5. 계정별 인기 콘텐츠
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("accountPopularContent", () -> {
                accountPopularContentService.generateAccountPopularContent(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_my_popular_content WHERE snapshot_date = ?",
                        Long.class, targetDate);
            }, contentCount)));

            // 6. 트렌딩 인플루언서
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("trendingInfluencer", () -> {
                sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType, targetDate, accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_trending_influencer WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, accountCount)));

            // 7. 트렌딩 콘텐츠
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("trendingContent", () -> {
                sparkTrendingContentService.generateDailyTrendingContent(platformType, targetDate, contentData,
                        accountData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_trending_content WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount + accountCount)));

            // 8. 키워드 트렌드
            futures.add(CompletableFuture.supplyAsync(() -> measureSingleInsight("keywordTrend", () -> {
                sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate, contentData);
                return jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM daily_keyword_trend WHERE platform_type_id = ? AND snapshot_date = ?",
                        Long.class, platformType.toUpperCase(), targetDate);
            }, contentCount)));

            // 모든 작업 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 결과 수집
            List<InsightBenchmarkResult> results = new ArrayList<>();
            for (CompletableFuture<InsightBenchmarkResult> future : futures) {
                results.add(future.get());
            }

            // 캐시 해제
            contentData.unpersist();
            accountData.unpersist();

            long totalEndTime = System.currentTimeMillis();

            AllInsightsBenchmarkResult finalResult = AllInsightsBenchmarkResult.builder()
                    .totalExecutionTimeMs(totalEndTime - totalStartTime)
                    .totalInsightsCount(results.size())
                    .insights(results)
                    .build();

            finalResult.calculateAverage();
            finalResult.setSummary(String.format(
                    "%d개 인사이트 병렬 분석 완료, 평균 %.2f초/인사이트",
                    results.size(),
                    finalResult.getAverageTimePerInsight() / 1000.0));

            log.info("=== 전체 인사이트 병렬 분석 완료: {}ms ===", finalResult.getTotalExecutionTimeMs());

            return finalResult;

        } catch (Exception e) {
            log.error("인사이트 병렬 분석 측정 실패", e);
            throw new RuntimeException("인사이트 병렬 분석 측정 실패", e);
        }
    }

    /**
     * 단일 인사이트 분석 성능 측정
     */
    private InsightBenchmarkResult measureSingleInsight(String insightName, InsightTask task, long recordsProcessed) {
        log.info(">>> {} 측정 시작", insightName);

        long startTime = System.currentTimeMillis();

        try {
            long recordsSaved = task.execute();
            long endTime = System.currentTimeMillis();

            InsightBenchmarkResult result = InsightBenchmarkResult.builder()
                    .insightName(insightName)
                    .executionTimeMs(endTime - startTime)
                    .recordsProcessed(recordsProcessed)
                    .recordsSaved(recordsSaved)
                    .status("SUCCESS")
                    .build();

            result.calculateThroughput();

            log.info(">>> {} 완료: {}ms, {} records processed, {} records saved",
                    insightName,
                    result.getExecutionTimeMs(),
                    result.getRecordsProcessed(),
                    result.getRecordsSaved());

            return result;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error(">>> {} 실패", insightName, e);

            return InsightBenchmarkResult.builder()
                    .insightName(insightName)
                    .executionTimeMs(endTime - startTime)
                    .recordsProcessed(recordsProcessed)
                    .recordsSaved(0)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 인사이트 작업 인터페이스
     */
    @FunctionalInterface
    private interface InsightTask {
        Long execute() throws Exception;
    }
}
