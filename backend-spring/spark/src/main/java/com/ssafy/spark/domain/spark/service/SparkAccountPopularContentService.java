package com.ssafy.spark.domain.spark.service;

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
public class SparkAccountPopularContentService extends SparkBaseService{

  /**
   * 각 계정별 인기 콘텐츠 Top 3 생성
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param categoryName 카테고리 타입(예 : "뷰티", "게임")
   * @param targetDate 통계를 생성할 기준 날짜
   */
  public void generateAccountPopularContent(String platformType, String categoryName, LocalDate targetDate) {
    try {
      // 1. 콘텐츠 데이터 읽기
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate);

      // 2. 계정 데이터 읽기
      Dataset<Row> accountData = readS3AccountData(platformType)
          .select("accountNickname", "categoryName");

      // 3. 조인하여 카테고리 필터링
      Dataset<Row> joinedData = contentData
          .join(accountData, "accountNickname") // 공통 컬럼 기준으로 equi-join
          .filter(col("categoryName").isNotNull()
              .and(col("categoryName").equalTo(categoryName)));

      // 4. 각 계정별로 조회수 기준 Top 3 추출
      Dataset<Row> accountTop3 = joinedData
          .withColumn("contentRank", row_number().over(
              org.apache.spark.sql.expressions.Window
                  .partitionBy("accountNickname")  // 계정별로 분할
                  .orderBy(col("viewsCount").desc())  // 조회수 내림차순
          ))
          .filter(col("contentRank").leq(3));  // Top 3만 선택

      // 5. 결과를 MySQL에 저장
      List<Row> results = accountTop3.collectAsList();

      log.info("[{}] 카테고리={} 각 계정별 Top3 콘텐츠 개수: {}", platformType, categoryName, results.size());

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType, accountNickname);
        Integer contentId = getContentId(platformType, externalContentId);
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer contentRank = row.getAs("contentRank");

        // 1) MySQL에 저장
        saveAccountPopularContent(platformType, categoryTypeId, row, targetDate, contentId, contentRank, platformAccountId);

        // 2) S3에도 저장
        saveAccountPopularContentToS3(platformType, categoryName, row, targetDate, contentId, contentRank, externalContentId, platformAccountId,accountNickname);
      }

    } catch (Exception e) {
      throw new RuntimeException("계정별 인기 콘텐츠 생성 실패", e);
    }
  }

  private void saveAccountPopularContentToS3(String platformType, String categoryName, Row row, LocalDate targetDate,
      Integer contentId, Integer contentRank, String externalContentId, Integer platformAccountId, String accountNickname) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformType", platformType);
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("contentId", contentId);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("contentRank", contentRank);
      statisticsJson.put("externalContentId", externalContentId);
      statisticsJson.put("accountNickname",accountNickname);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String fileName = String.format("account_popular_%s_%s_%s_%s.json",
          platformType, externalContentId, categoryName, timestamp);
      String s3Path = String.format("processed_data/%s/account_popular_content/%s/%s", platformType, dateFolder, fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 계정별 인기콘텐츠 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 계정별 인기콘텐츠 저장 실패: platform={}, externalContentId={}",
          platformType, externalContentId, e);
    }
  }

  private void saveAccountPopularContent(String platform, Integer categoryTypeId, Row row, LocalDate targetDate,
      Integer contentId, Integer contentRank, Integer platformAccountId) {
    try {
      // 1. MySQL INSERT/UPDATE 쿼리
      String sql = "INSERT INTO daily_my_popular_content " +
          "( platform_account_id, content_id, content_rank, snapshot_date, created_at) " +
          "VALUES ( ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "content_rank = VALUES(content_rank), " +
          "created_at = VALUES(created_at)";

      // 2. 파라미터 바인딩
      jdbcTemplate.update(sql,
          platformAccountId,
          contentId,
          contentRank,
          targetDate,
          LocalDateTime.now()
      );

    } catch (Exception e) {
      log.error("계정별 인기콘텐츠 저장 실패", e);
    }
  }

}
