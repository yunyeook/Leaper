package com.ssafy.spark.domain.insight.controller;

import com.ssafy.spark.domain.insight.service.AccountInsightService;
import com.ssafy.spark.domain.insight.service.AccountPopularContentService;
import com.ssafy.spark.domain.insight.service.SparkBaseService;
import com.ssafy.spark.domain.insight.service.SparkKeywordTrendService;
import com.ssafy.spark.domain.insight.service.SparkPopularContentService;
import com.ssafy.spark.domain.insight.service.SparkPopularInfluencerService;
import com.ssafy.spark.domain.insight.service.SparkTrendingContentService;
import com.ssafy.spark.domain.insight.service.SparkTrendingInfluencerService;
import com.ssafy.spark.domain.insight.service.SparkTypeInsightService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
// @RestController // 임시 비활성화: 성능 측정 테스트용
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

        private final AccountInsightService accountInsightService;
        private final SparkTypeInsightService sparkTypeInsightService;
        private final SparkPopularContentService sparkPopularContentService;
        private final SparkPopularInfluencerService sparkPopularInfluencerService;
        private final AccountPopularContentService accountPopularContentService;
        private final SparkTrendingInfluencerService sparkTrendingInfluencerService;
        private final SparkTrendingContentService sparkTrendingContentService;
        private final SparkKeywordTrendService sparkKeywordTrendService;
        private final SparkBaseService sparkBaseService;

        /**
         * JSON 데이터를 Parquet으로 변환
         */
        @PostMapping("/convert/parquet")
        public ResponseEntity<String> convertToParquet(
                        @RequestParam(defaultValue = "instagram") String platformType,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                try {
                        if (targetDate == null) {
                                // targetDate = LocalDate.now();
                                targetDate = LocalDate.of(2025, 9, 28);
                        }

                        log.info("Parquet 변환 시작: platform={}, date={}", platformType, targetDate);

                        sparkBaseService.convertContentJsonToParquet(platformType, targetDate);
                        sparkBaseService.convertPlatformAccountJsonToParquet(platformType, targetDate);

                        log.info("Parquet 변환 완료: platform={}, date={}", platformType, targetDate);

                        return ResponseEntity.ok(String.format(
                                        "✅ Parquet 변환 완료\n- Platform: %s\n- Date: %s\n- Content: JSON → Parquet\n- Account: JSON → Parquet",
                                        platformType, targetDate));
                } catch (Exception e) {
                        log.error("Parquet 변환 실패: platform={}, date={}", platformType, targetDate, e);
                        return ResponseEntity.status(500)
                                        .body("❌ Parquet 변환 실패: " + e.getMessage());
                }
        }

        /**
         * 모든 인사이트 통계 생성 (동기)
         */
        @GetMapping("/testTotal")
        public ResponseEntity<String> totalInsights(
                        @RequestParam(defaultValue = "instagram") String platformType,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                try {
                        if (targetDate == null) {
                                // targetDate = LocalDate.now();
                                targetDate = LocalDate.of(2025, 9, 28);
                        }

                        final LocalDate date = targetDate;

                        log.info("통계 생성 시작: platform={}, date={}", platformType, date);

                        // Parquet 데이터 읽기 및 캐싱
                        Dataset<Row> contentData = sparkBaseService.readS3ContentDataByDate(platformType, date).cache();
                        Dataset<Row> accountData = sparkBaseService.readS3AccountData(platformType, date).cache();

                        log.info("데이터 로드 완료 - Content: {} rows, Account: {} rows",
                                        contentData.count(), accountData.count());

                        // 모든 작업을 비동기로 병렬 실행
                        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> accountInsightService
                                        .generateDailyAccountInsight(platformType, date, contentData, accountData));

                        CompletableFuture<Void> task2 = CompletableFuture
                                        .runAsync(() -> sparkTypeInsightService.generateDailyTypeInsight(platformType,
                                                        date, contentData));

                        CompletableFuture<Void> task3 = CompletableFuture.runAsync(() -> sparkPopularContentService
                                        .generateDailyPopularContent(platformType, date, contentData, accountData));

                        CompletableFuture<Void> task4 = CompletableFuture.runAsync(() -> sparkPopularInfluencerService
                                        .generateDailyPopularInfluencer(platformType, date, contentData, accountData));

                        CompletableFuture<Void> task5 = CompletableFuture.runAsync(
                                        () -> accountPopularContentService.generateAccountPopularContent(platformType,
                                                        date, contentData));

                        CompletableFuture<Void> task6 = CompletableFuture.runAsync(() -> sparkTrendingInfluencerService
                                        .generateDailyTrendingInfluencer(platformType, date, accountData));

                        CompletableFuture<Void> task7 = CompletableFuture.runAsync(() -> sparkTrendingContentService
                                        .generateDailyTrendingContent(platformType, date, contentData, accountData));

                        CompletableFuture<Void> task8 = CompletableFuture.runAsync(
                                        () -> sparkKeywordTrendService.generateDailyKeywordTrend(platformType, date,
                                                        contentData));

                        // 모든 작업이 완료될 때까지 대기
                        CompletableFuture.allOf(task1, task2, task3, task4, task5, task6, task7, task8).join();

                        // 캐시 해제
                        contentData.unpersist();
                        accountData.unpersist();

                        log.info("통계 생성 완료: platform={}, date={}", platformType, date);

                        return ResponseEntity.ok(String.format(
                                        "✅ 통계 생성 완료\n- Platform: %s\n- Date: %s\n- Tasks: 8개 완료",
                                        platformType, date));

                } catch (Exception e) {
                        log.error("통계 생성 실패: platform={}, date={}", platformType, targetDate, e);
                        return ResponseEntity.status(500).body("❌ 통계 생성 실패: " + e.getMessage());
                }
        }

        /**
         * 모든 인사이트 통계 생성 (비동기, 즉시 응답)
         */
        @GetMapping("/testTotalAsync")
        public ResponseEntity<String> totalInsightsAsync(
                        @RequestParam(defaultValue = "instagram") String platformType,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                if (targetDate == null) {
                        // targetDate = LocalDate.now();
                        targetDate = LocalDate.of(2025, 9, 28);
                }

                final LocalDate date = targetDate;

                log.info("비동기 통계 생성 시작: platform={}, date={}", platformType, date);

                // 비동기 작업 시작하고 즉시 응답 반환
                CompletableFuture.runAsync(() -> {
                        try {
                                // Parquet 데이터 읽기 및 캐싱
                                Dataset<Row> contentData = sparkBaseService.readS3ContentDataByDate(platformType, date)
                                                .cache();
                                Dataset<Row> accountData = sparkBaseService.readS3AccountData(platformType, date)
                                                .cache();

                                log.info("데이터 로드 완료 - Content: {} rows, Account: {} rows",
                                                contentData.count(), accountData.count());

                                List<CompletableFuture<Void>> futures = Arrays.asList(
                                                CompletableFuture.runAsync(() -> accountInsightService
                                                                .generateDailyAccountInsight(platformType,
                                                                                date, contentData, accountData)),
                                                CompletableFuture.runAsync(() -> sparkTypeInsightService
                                                                .generateDailyTypeInsight(platformType,
                                                                                date, contentData)),
                                                CompletableFuture.runAsync(() -> sparkPopularContentService
                                                                .generateDailyPopularContent(platformType, date,
                                                                                contentData, accountData)),
                                                CompletableFuture.runAsync(() -> sparkPopularInfluencerService
                                                                .generateDailyPopularInfluencer(platformType, date,
                                                                                contentData, accountData)),
                                                CompletableFuture.runAsync(() -> accountPopularContentService
                                                                .generateAccountPopularContent(platformType, date,
                                                                                contentData)),
                                                CompletableFuture.runAsync(() -> sparkTrendingInfluencerService
                                                                .generateDailyTrendingInfluencer(platformType, date,
                                                                                accountData)),
                                                CompletableFuture.runAsync(() -> sparkTrendingContentService
                                                                .generateDailyTrendingContent(platformType, date,
                                                                                contentData, accountData)),
                                                CompletableFuture.runAsync(() -> sparkKeywordTrendService
                                                                .generateDailyKeywordTrend(platformType, date,
                                                                                contentData)));

                                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                                // 캐시 해제
                                contentData.unpersist();
                                accountData.unpersist();

                                log.info("✅ 모든 비동기 통계 생성 완료: platform={}, date={}", platformType, date);

                        } catch (Exception e) {
                                log.error("❌ 비동기 통계 생성 중 오류 발생: platform={}, date={}", platformType, date, e);
                        }
                });

                return ResponseEntity.accepted()
                                .body(String.format(
                                                "🚀 통계 생성 작업이 시작되었습니다\n- Platform: %s\n- Date: %s\n- 백그라운드에서 실행 중...",
                                                platformType, date));
        }

        private final org.springframework.batch.core.launch.JobLauncher jobLauncher;
        private final org.springframework.batch.core.Job dailyInsightJob;

        /**
         * Spring Batch Job 실행
         */
        @GetMapping("/batch/total")
        public ResponseEntity<String> runBatchJob(
                        @RequestParam(defaultValue = "instagram") String platformType,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
                try {
                        if (targetDate == null) {
                                targetDate = LocalDate.of(2025, 9, 28);
                        }

                        String dateStr = targetDate.toString();
                        log.info("Batch Job 실행 요청: platform={}, date={}", platformType, dateStr);

                        org.springframework.batch.core.JobParameters jobParameters = new org.springframework.batch.core.JobParametersBuilder()
                                        .addString("platformType", platformType)
                                        .addString("targetDate", dateStr)
                                        .addLong("time", System.currentTimeMillis()) // 중복 실행 방지용
                                        .toJobParameters();

                        jobLauncher.run(dailyInsightJob, jobParameters);

                        return ResponseEntity.ok("Batch Job Started: " + platformType + ", " + dateStr);

                } catch (Exception e) {
                        log.error("Batch Job 실행 실패", e);
                        return ResponseEntity.status(500).body("Batch Job Failed: " + e.getMessage());
                }
        }
}
