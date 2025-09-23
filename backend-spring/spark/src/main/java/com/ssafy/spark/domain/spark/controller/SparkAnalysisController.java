package com.ssafy.spark.domain.spark.controller;

import com.ssafy.spark.domain.spark.service.SparkAccountInsightService;
import com.ssafy.spark.domain.spark.service.SparkAccountPopularContentService;
import com.ssafy.spark.domain.spark.service.SparkDummyDataGeneratorService;
import com.ssafy.spark.domain.spark.service.SparkKeywordTrendService;
import com.ssafy.spark.domain.spark.service.SparkPopularContentService;
import com.ssafy.spark.domain.spark.service.SparkPopularInfluencerService;
import com.ssafy.spark.domain.spark.service.SparkTrendingContentService;
import com.ssafy.spark.domain.spark.service.SparkTrendingInfluencerService;
import com.ssafy.spark.domain.spark.service.SparkTypeInsightService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  private final SparkDummyDataGeneratorService sparkDummyDataGeneratorService;

  //TODO : 지금 인스타그램으로 고정되어있기 때문에 유튜브도 확인하기!!
  @GetMapping("/testTotal")  // TODO : 날짜 잘 확인하기!! snapshot이랑 s3 저장 경로에 영향을 주고 있음.
  public ResponseEntity<String> totalInsights(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
//      targetDate = LocalDate.now();
      targetDate= LocalDate.of(2025, 9, 23);
    }

    sparkAccountInsightService.generateDailyAccountInsight(platformType, targetDate);
    sparkTypeInsightService.generateDailyTypeInsight(platformType, targetDate);
    sparkPopularContentService.generateDailyPopularContent(platformType,targetDate);
    sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType,targetDate);
    sparkAccountPopularContentService.generateAccountPopularContent(platformType, targetDate);
    sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType,targetDate);
    sparkTrendingContentService.generateDailyTrendingContent(platformType,targetDate);
    sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate);



    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }
  @GetMapping("/test")
  public ResponseEntity<String> dailyAccountInsight(
      @RequestParam(defaultValue = "instagram") String platform,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkAccountInsightService.generateDailyAccountInsight(platform, targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platform + ", " + targetDate);
  }

  @GetMapping("/test2")
  public ResponseEntity<String> dailyTypeInsight(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
//      targetDate = LocalDate.now();
      targetDate= LocalDate.of(2025, 9, 22);

    }

    sparkKeywordTrendService.generateDailyKeywordTrend(platformType, targetDate);
//    sparkTypeInsightService.generateDailyTypeInsight(platform, targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }



  @GetMapping("/test3")
  public ResponseEntity<String> dailyPopularContent(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkPopularContentService.generateDailyPopularContent(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }
  @GetMapping("/test7")
  public ResponseEntity<String> dailyTrendingContent(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkTrendingContentService.generateDailyTrendingContent(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  @GetMapping("/test4")
  public ResponseEntity<String> dailyPopularInfluencer(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkPopularInfluencerService.generateDailyPopularInfluencer(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  @GetMapping("/test6")
  public ResponseEntity<String> dailyTrendingInfluencer(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkTrendingInfluencerService.generateDailyTrendingInfluencer(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  @GetMapping("/test5")
  public ResponseEntity<String> dailyAccountPopularContent(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkAccountPopularContentService.generateAccountPopularContent(platformType, targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  /**
   * 365일전까지 더미데이터 생성
   */
  @GetMapping("/dummy")
  public ResponseEntity<String> dummy365(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate= LocalDate.of(2025, 9, 22); //TODO :  더비 시작날짜로 날짜 수정하기
     }
    sparkDummyDataGeneratorService.generateAllDummyData(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  @GetMapping("/check-s3")
  public ResponseEntity<String> checkS3File() {
    // 최근 생성된 파일 경로 (실제 파일명으로 수정)
    String s3Path = "processed_data/instagram/daily_account_insight/daily_stats_instagram_46597603342_20250916140432.json";
    sparkAccountInsightService.readAndLogS3File(s3Path);
    return ResponseEntity.ok("S3 파일 내용을 로그에서 확인하세요");
  }

}
