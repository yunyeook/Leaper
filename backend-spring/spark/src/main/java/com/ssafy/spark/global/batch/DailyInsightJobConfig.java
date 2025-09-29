package com.ssafy.spark.global.batch;

import com.ssafy.spark.domain.insight.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@Slf4j
// @Configuration // 임시 비활성화: 성능 측정 테스트용
@RequiredArgsConstructor
public class DailyInsightJobConfig {

        private final JobBuilderFactory jobBuilderFactory;
        private final StepBuilderFactory stepBuilderFactory;

        private final SparkBaseService sparkBaseService;
        private final AccountInsightService accountInsightService;
        private final SparkTypeInsightService sparkTypeInsightService;
        private final SparkPopularContentService sparkPopularContentService;
        private final SparkPopularInfluencerService sparkPopularInfluencerService;
        private final AccountPopularContentService accountPopularContentService;
        private final SparkTrendingInfluencerService sparkTrendingInfluencerService;
        private final SparkTrendingContentService sparkTrendingContentService;
        private final SparkKeywordTrendService sparkKeywordTrendService;

        // JobParameters에서 날짜와 플랫폼을 받기 위해 멤버 변수로 저장할 수도 있지만,
        // StepScope를 사용하여 각 Step에서 파라미터를 주입받는 것이 더 안전합니다.
        // 하지만 여기서는 Spark Dataset을 공유해야 하므로,
        // 1. PreparationStep에서 로드 후 static 변수나 싱글톤 빈에 저장 (간단하지만 위험)
        // 2. 각 Step에서 다시 로드 (Spark 캐싱 활용) -> 이 방식이 안전함. SparkSession이 살아있고 cache()를
        // 호출했으므로.

        @Bean
        public Job dailyInsightJob() {
                return jobBuilderFactory.get("dailyInsightJob")
                                .start(preparationStep())
                                // 병렬 처리(Flow)에서 순차 처리(Step by Step)로 변경 (안정성 확보)
                                .next(accountInsightStep())
                                .next(typeInsightStep())
                                .next(popularContentStep())
                                .next(popularInfluencerStep())
                                .next(accountPopularContentStep())
                                .next(trendingInfluencerStep())
                                .next(trendingContentStep())
                                .next(keywordTrendStep())
                                .next(cleanupStep())
                                .build();
        }

        @Bean
        @JobScope
        public Step preparationStep() {
                return stepBuilderFactory.get("preparationStep")
                                .tasklet((contribution, chunkContext) -> {
                                        String platformType = (String) chunkContext.getStepContext().getJobParameters()
                                                        .get("platformType");
                                        String dateStr = (String) chunkContext.getStepContext().getJobParameters()
                                                        .get("targetDate");
                                        LocalDate targetDate = LocalDate.parse(dateStr);

                                        log.info(">>> [PreparationStep] 데이터 로드 및 캐싱 시작: platform={}, date={}",
                                                        platformType, targetDate);

                                        // SparkBaseService를 통해 데이터 로드 및 캐싱
                                        // 주의: 여기서 cache()를 호출하면 SparkSession 내에 캐시됨.
                                        // 이후 Step에서 동일한 경로로 read()를 호출하면 캐시된 데이터를 사용함.
                                        sparkBaseService.readS3ContentDataByDate(platformType, targetDate).cache()
                                                        .count(); // count()로 액션
                                                                  // 트리거
                                        sparkBaseService.readS3AccountData(platformType, targetDate).cache().count();

                                        log.info(">>> [PreparationStep] 데이터 로드 및 캐싱 완료");
                                        return RepeatStatus.FINISHED;
                                })
                                .build();
        }

        @Bean
        @JobScope
        public Step accountInsightStep() {
                return stepBuilderFactory.get("accountInsightStep")
                                .tasklet(analysisTasklet((platformType, targetDate, contentData,
                                                accountData) -> accountInsightService
                                                                .generateDailyAccountInsight(platformType, targetDate,
                                                                                contentData, accountData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step typeInsightStep() {
                return stepBuilderFactory.get("typeInsightStep")
                                .tasklet(analysisTasklet((platformType, targetDate, contentData,
                                                accountData) -> sparkTypeInsightService
                                                                .generateDailyTypeInsight(platformType, targetDate,
                                                                                contentData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step popularContentStep() {
                return stepBuilderFactory.get("popularContentStep")
                                .tasklet(analysisTasklet(
                                                (platformType, targetDate, contentData,
                                                                accountData) -> sparkPopularContentService
                                                                                .generateDailyPopularContent(
                                                                                                platformType,
                                                                                                targetDate, contentData,
                                                                                                accountData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step popularInfluencerStep() {
                return stepBuilderFactory.get("popularInfluencerStep")
                                .tasklet(analysisTasklet(
                                                (platformType, targetDate, contentData,
                                                                accountData) -> sparkPopularInfluencerService
                                                                                .generateDailyPopularInfluencer(
                                                                                                platformType,
                                                                                                targetDate,
                                                                                                accountData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step accountPopularContentStep() {
                return stepBuilderFactory.get("accountPopularContentStep")
                                .tasklet(analysisTasklet(
                                                (platformType, targetDate, contentData,
                                                                accountData) -> accountPopularContentService
                                                                                .generateAccountPopularContent(
                                                                                                platformType,
                                                                                                targetDate,
                                                                                                contentData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step trendingInfluencerStep() {
                return stepBuilderFactory.get("trendingInfluencerStep")
                                .tasklet(analysisTasklet(
                                                (platformType, targetDate, contentData,
                                                                accountData) -> sparkTrendingInfluencerService
                                                                                .generateDailyTrendingInfluencer(
                                                                                                platformType,
                                                                                                targetDate,
                                                                                                accountData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step trendingContentStep() {
                return stepBuilderFactory.get("trendingContentStep")
                                .tasklet(analysisTasklet(
                                                (platformType, targetDate, contentData,
                                                                accountData) -> sparkTrendingContentService
                                                                                .generateDailyTrendingContent(
                                                                                                platformType,
                                                                                                targetDate, contentData,
                                                                                                accountData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step keywordTrendStep() {
                return stepBuilderFactory.get("keywordTrendStep")
                                .tasklet(
                                                analysisTasklet((platformType, targetDate, contentData,
                                                                accountData) -> sparkKeywordTrendService
                                                                                .generateDailyKeywordTrend(platformType,
                                                                                                targetDate,
                                                                                                contentData)))
                                .build();
        }

        @Bean
        @JobScope
        public Step cleanupStep() {
                return stepBuilderFactory.get("cleanupStep")
                                .tasklet((contribution, chunkContext) -> {
                                        String platformType = (String) chunkContext.getStepContext().getJobParameters()
                                                        .get("platformType");
                                        String dateStr = (String) chunkContext.getStepContext().getJobParameters()
                                                        .get("targetDate");
                                        LocalDate targetDate = LocalDate.parse(dateStr);

                                        log.info(">>> [CleanupStep] 캐시 해제 시작");
                                        // 다시 read 호출하여 객체 얻고 unpersist
                                        sparkBaseService.readS3ContentDataByDate(platformType, targetDate).unpersist();
                                        sparkBaseService.readS3AccountData(platformType, targetDate).unpersist();
                                        log.info(">>> [CleanupStep] 캐시 해제 완료");
                                        return RepeatStatus.FINISHED;
                                })
                                .build();
        }

        // 함수형 인터페이스 정의
        @FunctionalInterface
        interface AnalysisAction {
                void execute(String platformType, LocalDate targetDate, Dataset<Row> contentData,
                                Dataset<Row> accountData);
        }

        private Tasklet analysisTasklet(AnalysisAction action) {
                return (contribution, chunkContext) -> {
                        String platformType = (String) chunkContext.getStepContext().getJobParameters()
                                        .get("platformType");
                        String dateStr = (String) chunkContext.getStepContext().getJobParameters().get("targetDate");
                        LocalDate targetDate = LocalDate.parse(dateStr);

                        log.info(">>> Step 시작: {}", chunkContext.getStepContext().getStepName());

                        // SparkSession에서 캐시된 데이터프레임 가져오기 (경로가 같으면 캐시된 것 사용)
                        Dataset<Row> contentData = sparkBaseService.readS3ContentDataByDate(platformType, targetDate);
                        Dataset<Row> accountData = sparkBaseService.readS3AccountData(platformType, targetDate);

                        action.execute(platformType, targetDate, contentData, accountData);

                        log.info(">>> Step 완료: {}", chunkContext.getStepContext().getStepName());
                        return RepeatStatus.FINISHED;
                };
        }
}
