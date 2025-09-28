package com.ssafy.spark.domain.analysis.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkKeywordTrendService extends SparkBaseService {

  /**
   * 일별 키워드 트렌드 생성
   */
  public void generateDailyKeywordTrend(String platformType, LocalDate targetDate) {
    try {
      // 1. 오늘 크롤링된 콘텐츠 데이터 읽기
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate);

      // 2. tags 필드에서 키워드 추출 및 집계
      Dataset<Row> keywordStats = contentData
          .filter(col("tags").isNotNull())
          .select(explode(col("tags")).alias("keyword"))
          .filter(col("keyword").isNotNull())
          .filter(length(col("keyword")).gt(1)) // 1글자 키워드 제외
          .groupBy("keyword")
          .agg(count("*").alias("frequency"))
          .orderBy(desc("frequency"))
          .limit(10); // 상위 10개만

      // 3. 결과를 List로 변환
      List<Row> topKeywords = keywordStats.collectAsList();

      if (topKeywords.isEmpty()) {
        log.warn("키워드 트렌드 데이터가 없습니다 - Date: {}, Platform: {}", targetDate, platformType);
        return;
      }

      log.info("키워드 트렌드 생성 완료 - Date: {}, Platform: {}, 키워드 수: {}",
          targetDate, platformType, topKeywords.size());

      // 4. DB 저장
      saveDailyKeywordTrend(platformType, topKeywords, targetDate);

      // 5. S3 저장
      saveKeywordTrendToS3(platformType, topKeywords, targetDate);

    } catch (Exception e) {
      log.error("키워드 트렌드 생성 실패 - Date: {}, Platform: {}", targetDate, platformType, e);
      throw new RuntimeException("키워드 트렌드 생성 실패", e);
    }
  }

  /**
   * DB 저장
   */
  private void saveDailyKeywordTrend(String platformType, List<Row> topKeywords, LocalDate targetDate) {
    try {
      // 키워드 리스트 생성 (상위 10개)
      List<String> keywords = topKeywords.stream()
          .map(row -> row.getAs("keyword").toString())
          .collect(Collectors.toList());

      String sql = "INSERT INTO daily_keyword_trend " +
          "(platform_type_id, keywords_json, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "keywords_json = VALUES(keywords_json), " +
          "created_at = VALUES(created_at)";

      String keywordsJson = objectMapper.writeValueAsString(keywords);

      jdbcTemplate.update(sql,
          platformType.toUpperCase(),
          keywordsJson,
          targetDate,
          LocalDateTime.now());

      log.info("키워드 트렌드 DB 저장 완료 - Date: {}, Keywords: {}", targetDate, keywords);

    } catch (Exception e) {
      log.error("키워드 트렌드 DB 저장 실패", e);
    }
  }

  /**
   * S3 저장
   */
  private void saveKeywordTrendToS3(String platformType, List<Row> topKeywords, LocalDate targetDate) {
    try {
      ObjectNode trendJson = objectMapper.createObjectNode();
      trendJson.put("platformType", platformType.toUpperCase());
      trendJson.put("snapshotDate", targetDate.toString());

      ArrayNode keywordArray = objectMapper.createArrayNode();
      for (Row row : topKeywords) {
        ObjectNode keywordObj = objectMapper.createObjectNode();
        keywordObj.put("keyword", row.getAs("keyword").toString());
        keywordObj.put("frequency", (Long) row.getAs("frequency"));
        keywordArray.add(keywordObj);
      }
      trendJson.set("topKeywords", keywordArray);
      trendJson.put("processedAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(trendJson);

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("keyword_trend_%s_%s.json", platformType, timestamp);
      String s3Path = String.format("processed_data/%s/daily_keyword_trend/%s/%s/%s/%s",
          platformType,
          targetDate.getYear(),
          String.format("%02d", targetDate.getMonthValue()),
          String.format("%02d", targetDate.getDayOfMonth()),
          fileName);

      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("키워드 트렌드 S3 저장 완료 - Path: {}", s3Path);

    } catch (Exception e) {
      log.error("키워드 트렌드 S3 저장 실패", e);
    }
  }
}