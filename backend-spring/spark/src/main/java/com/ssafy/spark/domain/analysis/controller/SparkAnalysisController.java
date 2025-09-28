package com.ssafy.spark.domain.analysis.controller;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.spark.domain.analysis.service.SparkAccountInsightService;
import com.ssafy.spark.domain.analysis.service.SparkAccountPopularContentService;
import com.ssafy.spark.domain.analysis.service.SparkDummyDataGeneratorService;
import com.ssafy.spark.domain.analysis.service.SparkKeywordTrendService;
import com.ssafy.spark.domain.analysis.service.SparkPopularContentService;
import com.ssafy.spark.domain.analysis.service.SparkPopularInfluencerService;
import com.ssafy.spark.domain.analysis.service.SparkTrendingContentService;
import com.ssafy.spark.domain.analysis.service.SparkTrendingInfluencerService;
import com.ssafy.spark.domain.analysis.service.SparkTypeInsightService;

@RestController
@RequestMapping("/spark")
@RequiredArgsConstructor
public class SparkAnalysisController {

  private final SparkAccountInsightService sparkAccountInsightService;
  private final SparkTypeInsightService sparkTypeInsightService;
  private final SparkPopularContentService sparkPopularContentService;
  private final SparkPopularInfluencerService sparkPopularInfluencerService;
  private final SparkAccountPopularContentService sparkAccountPopularContentService;
  private final SparkTrendingInfluencerService sparkTrendingInfluencerService;
  private final SparkTrendingContentService sparkTrendingContentService;
  private final SparkKeywordTrendService sparkKeywordTrendService;

  // TODO : 지금 인스타그램으로 고정되어있기 때문에 유튜브도 확인하기!!
  @GetMapping("/testTotal") // TODO : 날짜 잘 확인하기!! snapshot이랑 s3 저장 경로에 영향을 주고 있음.
  public ResponseEntity<String> totalInsights(
      @RequestParam(defaultValue = "youtube") String platformType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
      // targetDate= LocalDate.of(2025, 9, 27);
    }
    sparkAccountInsightService.generateDailyAccountInsight(platformType, targetDate);
    sparkTypeInsightService.generateDailyTypeInsight(platformType, targetDate);
    sparkPopularContentService.generateDailyPopularContent(platformType, targetDate);
    sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType, targetDate);
    sparkAccountPopularContentService.generateAccountPopularContent(platformType, targetDate);
    sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType, targetDate);
    sparkTrendingContentService.generateDailyTrendingContent(platformType, targetDate);
    sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate);
    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

}
