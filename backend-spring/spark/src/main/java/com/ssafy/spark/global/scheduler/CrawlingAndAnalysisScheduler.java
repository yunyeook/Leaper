//package com.ssafy.spark.global.scheduler;
//
//import com.ssafy.spark.domain.business.type.entity.PlatformType;
//import com.ssafy.spark.domain.business.type.repository.PlatformTypeRepository;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//import com.ssafy.spark.domain.spark.service.SparkAccountInsightService;
//import com.ssafy.spark.domain.spark.service.SparkAccountPopularContentService;
//import com.ssafy.spark.domain.spark.service.SparkKeywordTrendService;
//import com.ssafy.spark.domain.spark.service.SparkPopularContentService;
//import com.ssafy.spark.domain.spark.service.SparkPopularInfluencerService;
//import com.ssafy.spark.domain.spark.service.SparkTrendingContentService;
//import com.ssafy.spark.domain.spark.service.SparkTrendingInfluencerService;
//import com.ssafy.spark.domain.spark.service.SparkTypeInsightService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import org.springframework.web.client.RestTemplate;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class CrawlingAndAnalysisScheduler {
//  private final RestTemplate restTemplate = new RestTemplate();
//
//  private final SparkAccountInsightService sparkAccountInsightService;
//  private final SparkTypeInsightService sparkTypeInsightService;
//  private final SparkPopularContentService sparkPopularContentService;
//  private final SparkPopularInfluencerService sparkPopularInfluencerService;
//  private final SparkAccountPopularContentService sparkAccountPopularContentService;
//  private final SparkTrendingInfluencerService sparkTrendingInfluencerService;
//  private final SparkTrendingContentService sparkTrendingContentService;
//  private final SparkKeywordTrendService sparkKeywordTrendService;
//  private final PlatformTypeRepository platformTypeRepository;
//
//  /**
//   * 매일 자정에 모든 플랫폼 계정 인사이트 분석 실행
//   */
//  @Scheduled(cron = "0 0 0 * * *") //초 분 시 일 월 요일
//  public void scheduledDailyAnalysis() {
//    log.info("스케줄된 일일 분석 시작");
//    List<PlatformType> platFormTypeList=platformTypeRepository.findAll();
//    for(PlatformType platformType : platFormTypeList){
//    try {
//      LocalDate targetDate = LocalDate.now();
//
//      executeAnalysis(platformType.getId().toLowerCase(), targetDate);
//
//      log.info("스케줄된 일일 분석 완료: {}, {}", platformType.getId().toLowerCase(), targetDate);
//    } catch (Exception e) {
//      log.error("스케줄된 일일 분석 중 오류 발생", e);
//    }
//  }
//  }
//
//
//  /**
//   * 테스트용 - 매 10분마다 실행 (개발 중에만 사용)
//   * 테스트할 때만 주석 해제하세요
//   */
//  // @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
//  public void scheduledTestAnalysis() {
//    log.info("테스트 스케줄 분석 시작");
//
//    try {
//      LocalDate targetDate = LocalDate.now();
//      String platformType = "youtube";
//
//      executeAnalysis(platformType, targetDate);
//
//      log.info("테스트 스케줄 분석 완료: {}, {}", platformType, targetDate);
//    } catch (Exception e) {
//      log.error("테스트 스케줄 분석 중 오류 발생", e);
//    }
//  }
//
//  /**
//   * 실제 분석 로직 실행 (기존 testTotal 엔드포인트 로직과 동일)
//   */
//  private void executeAnalysis(String platformType, LocalDate targetDate) {
//    log.info("분석 실행 시작: platformType={}, targetDate={}", platformType, targetDate);
//
//    sparkAccountInsightService.generateDailyAccountInsight(platformType, targetDate);
//    sparkTypeInsightService.generateDailyTypeInsight(platformType, targetDate);
//    sparkPopularContentService.generateDailyPopularContent(platformType, targetDate);
//    sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType, targetDate);
//    sparkAccountPopularContentService.generateAccountPopularContent(platformType, targetDate);
//    sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType, targetDate);
//    sparkTrendingContentService.generateDailyTrendingContent(platformType, targetDate);
//    sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate);
//
//    log.info("분석 실행 완료: platformType={}, targetDate={}", platformType, targetDate);
//  }
//}
