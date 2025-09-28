package com.ssafy.spark.domain.spark.controller;

import com.ssafy.spark.domain.spark.service.SparkDummyDataGeneratorService;
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
public class DummyInsightController {

  private final SparkDummyDataGeneratorService sparkDummyDataGeneratorService;/**
   * 365일전까지 더미데이터 생성
   */
  @GetMapping("/dummy")
  public ResponseEntity<String> dummy365(
      @RequestParam(defaultValue = "youtube") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    // targetDate가 null이면 오늘 날짜 사용
    if (targetDate == null) {
      targetDate= LocalDate.of(2025, 9, 25); //TODO :  더미 있는 시작날짜로 날짜 수정하기
    }
    sparkDummyDataGeneratorService.generateAllDummyData(platformType,targetDate);

    return ResponseEntity.ok("통계 생성 완료: " + platformType + ", " + targetDate);
  }

  //특정 계정 365 더미데이터.
  @GetMapping("/dummy/account/all")
  public ResponseEntity<String> dummyAccountAll365(
      @RequestParam(defaultValue = "youtube") String platformType,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
      @RequestParam Long platformAccountId
  ) {
    if (targetDate == null) {
      targetDate = LocalDate.of(2025, 9, 27); //TODO : 날짜 수정
    }
    sparkDummyDataGeneratorService.generateDummyForOneAccountAll(platformType, targetDate, platformAccountId);

    return ResponseEntity.ok("특정 계정 전체 더미 생성 완료: " + platformType + ", platformAccountId=" + platformAccountId);
  }
}
