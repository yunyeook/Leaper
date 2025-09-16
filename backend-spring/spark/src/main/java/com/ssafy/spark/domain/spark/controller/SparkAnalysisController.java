package com.ssafy.spark.domain.spark.controller;

import com.ssafy.spark.domain.spark.service.SparkAccountInsightService;
import com.ssafy.spark.domain.spark.service.SparkPopularContentService;
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
@RequiredArgsConstructor// ← 슬래시 추가
public class SparkAnalysisController {

  private final SparkAccountInsightService sparkAccountInsightService;
  private final SparkTypeInsightService sparkTypeInsightService;
  private final SparkPopularContentService sparkPopularContentService;


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
      @RequestParam(defaultValue = "instagram") String platform,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkTypeInsightService.generateDailyTypeInsight(platform, targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platform + ", " + targetDate);
  }



  @GetMapping("/test3")
  public ResponseEntity<String> dailyPopulaInsight(
      @RequestParam(defaultValue = "instagram") String platformType,
      @RequestParam(defaultValue = "뷰티") String categoryName,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate = LocalDate.now();
    }

    sparkPopularContentService.generateDailyPopularContent(platformType, categoryName,targetDate);

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
