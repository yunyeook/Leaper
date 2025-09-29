package com.ssafy.spark.domain.insight.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountPopularContentService extends SparkBaseService {

  /**
   * 각 계정별 인기 콘텐츠 Top 3 생성
   * 
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate   통계를 생성할 기준 날짜
   */
  public void generateAccountPopularContent(String platformType, LocalDate targetDate, Dataset<Row> contentData) {
    try {

      // 2. 각 계정별로 조회수 기준 Top 3 추출
      Dataset<Row> accountTop3 = contentData
          .withColumn("contentRank", row_number().over(
              org.apache.spark.sql.expressions.Window
                  .partitionBy("accountNickname") // 계정별로 분할
                  .orderBy(col("viewsCount").desc()) // 조회수 내림차순
          ))
          .filter(col("contentRank").leq(3)); // Top 3만 선택

      // . 결과를 MySQL에 저장
      List<Row> results = accountTop3.collectAsList();

      log.info("[{}] 각 계정별 Top3 콘텐츠 개수: {}", platformType, results.size());

      List<AccountPopularContentBatch> batchData = new java.util.ArrayList<>();

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);

        if (platformAccountId == null) {
          log.warn("PlatformAccount not found, skipping: platform={}, nickname={}", platformType, accountNickname);
          continue;
        }

        Integer contentId = getContentId(platformType.toUpperCase(), externalContentId);
        Integer contentRank = row.getAs("contentRank");

        // 1) Batch 데이터 수집
        batchData.add(new AccountPopularContentBatch(
            platformAccountId,
            contentId,
            contentRank,
            targetDate));

        // 1000개씩 끊어서 저장
        if (batchData.size() >= 1000) {
          saveAccountPopularContentBatch(batchData);
          batchData.clear();
        }

        // 2) S3에도 저장
        saveAccountPopularContentToS3(platformType, row, targetDate, contentId, contentRank, externalContentId,
            platformAccountId, accountNickname);
      }

      // 남은 데이터 저장
      if (!batchData.isEmpty()) {
        saveAccountPopularContentBatch(batchData);
      }

    } catch (Exception e) {
      throw new RuntimeException("계정별 인기 콘텐츠 생성 실패", e);
    }
  }

  /**
   * Batch Insert DTO
   */
  @lombok.AllArgsConstructor
  private static class AccountPopularContentBatch {
    Integer platformAccountId;
    Integer contentId;
    Integer contentRank;
    LocalDate snapshotDate;
  }

  /**
   * JDBC Batch Update
   */
  private void saveAccountPopularContentBatch(List<AccountPopularContentBatch> batchData) {
    if (batchData.isEmpty())
      return;

    String sql = "INSERT INTO daily_my_popular_content " +
        "( platform_account_id, content_id, content_rank, snapshot_date, created_at) " +
        "VALUES ( ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "content_rank = VALUES(content_rank), " +
        "created_at = VALUES(created_at)";

    jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
      @Override
      public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
        AccountPopularContentBatch dto = batchData.get(i);
        ps.setInt(1, dto.platformAccountId);
        ps.setInt(2, dto.contentId);
        ps.setInt(3, dto.contentRank);
        ps.setObject(4, dto.snapshotDate);
        ps.setObject(5, LocalDateTime.now());
      }

      @Override
      public int getBatchSize() {
        return batchData.size();
      }
    });
  }

  private void saveAccountPopularContentToS3(String platformType, Row row, LocalDate targetDate,
      Integer contentId, Integer contentRank, String externalContentId, Integer platformAccountId,
      String accountNickname) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("contentId", contentId);
      statisticsJson.put("contentRank", contentRank);
      statisticsJson.put("externalContentId", externalContentId);
      statisticsJson.put("accountNickname", accountNickname);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로 
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_account_popular_content_%s_%s.json",
          contentId, timestamp);
      String s3Path = String.format("processed_data/json/%s/daily_account_popular_content/%s/%s", platformType,
          dateFolder, fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 계정별 인기콘텐츠 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 계정별 인기콘텐츠 저장 실패: platform={}, externalContentId={}",
          platformType, externalContentId, e);
    }
  }

  /**
   * 특정 계정의 인기 콘텐츠 Top 3 생성 (오버로드)
   * 
   * @param platformType              플랫폼 타입
   * @param targetDate                통계를 생성할 기준 날짜
   * @param specificPlatformAccountId 특정 계정 ID
   */
  public void generateAccountPopularContent(String platformType, LocalDate targetDate,
      Integer specificPlatformAccountId) {
    try {
      // 특정 계정의 닉네임 조회
      String specificAccountNickname = getAccountNickname(specificPlatformAccountId);
      if (specificAccountNickname == null) {
        log.warn("계정을 찾을 수 없음: platformAccountId={}", specificPlatformAccountId);
        return;
      }

      // 1. 해당 계정의 콘텐츠만 필터링해서 읽기
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate)
          .filter(col("accountNickname").equalTo(specificAccountNickname))
          .cache();

      // 2. 해당 계정의 조회수 기준 Top 3 추출
      Dataset<Row> accountTop3 = contentData
          .withColumn("contentRank", row_number().over(
              org.apache.spark.sql.expressions.Window
                  .partitionBy("accountNickname")
                  .orderBy(col("viewsCount").desc())))
          .filter(col("contentRank").leq(3));

      // 3. 결과 저장
      List<Row> results = accountTop3.collectAsList();

      if (results.isEmpty()) {
        log.warn("해당 계정의 콘텐츠를 찾을 수 없음: {}", specificAccountNickname);
        return;
      }

      log.info("특정 계정 인기 콘텐츠 개수: {}", results.size());

      List<AccountPopularContentBatch> batchData = new java.util.ArrayList<>();

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        Integer contentId = getContentId(platformType.toUpperCase(), externalContentId);
        Integer contentRank = row.getAs("contentRank");

        // 1) Batch 데이터 수집
        batchData.add(new AccountPopularContentBatch(
            specificPlatformAccountId,
            contentId,
            contentRank,
            targetDate));

        // 2) S3에도 저장
        saveAccountPopularContentToS3(platformType, row, targetDate, contentId, contentRank,
            externalContentId, specificPlatformAccountId, specificAccountNickname);
      }

      // Batch 저장 실행
      saveAccountPopularContentBatch(batchData);

      log.info("특정 계정 인기 콘텐츠 생성 완료: accountId={}", specificPlatformAccountId);

    } catch (Exception e) {
      log.error("특정 계정 인기 콘텐츠 생성 실패: accountId={}", specificPlatformAccountId, e);
      throw new RuntimeException("특정 계정 인기 콘텐츠 생성 실패", e);
    }
  }

}
