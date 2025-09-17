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
public class SparkPopularContentService extends SparkBaseService {

  /**
   * DailyPopularContent 생성
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param categoryName 카테고리 타입(예 : "뷰티", "게임")
   * @param targetDate 통계를 생성할 기준 날짜
   */
  public void generateDailyPopularContent(String platformType, String categoryName, LocalDate targetDate) {
    try {
      // 1. 콘텐츠 데이터 읽기
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate);

      // 2. 계정 데이터 읽기
      Dataset<Row> accountData = readS3AccountData(platformType)
          .select("accountNickname", "categoryName");

      // 3. 조인하여 카테고리 필터링
      Dataset<Row> joined = contentData
          .join(accountData, contentData.col("accountNickname").equalTo(accountData.col("accountNickname")))
          .filter(col("categoryName").isNotNull()
              .and(col("categoryName").equalTo(categoryName.toString())));

      // 4. 조회수 기준 Top 10 추출
      Dataset<Row> top10 = joined
          .orderBy(col("viewsCount").desc())
          .limit(10)
          .withColumn("contentRank", row_number().over(        //contentRank 컬럼 추가하여 1부터 번호 매김
              org.apache.spark.sql.expressions.Window.orderBy(col("viewsCount").desc())    // 번호매길때, 조회수 큰 순으로
          ));

      // 5. 결과를 MySQL에 저장
      List<Row> results = top10.collectAsList();

      log.info("[{}] 카테고리={} Top10 콘텐츠 개수: {}", platformType, categoryName, results.size());

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType, accountNickname);
        Integer contentId = getContentId(platformType, externalContentId);
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer contentRank = row.getAs("contentRank");


        // 1) MySQL에 저장
        saveDailyPopularContent(platformType, categoryTypeId, row, targetDate,contentId,contentRank);

        // 2) S3에도 저장
        savePopularContentToS3(platformType, categoryName, row, targetDate,contentId,contentRank,externalContentId);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyPopularContent 생성 실패", e);
    }
  }

  private void savePopularContentToS3(String platformType, String categoryName, Row row, LocalDate targetDate,
    Integer contentId,Integer contentRank,String externalContentId
  ) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformType", platformType);
      statisticsJson.put("contentId", contentId);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("contentRank",contentRank);
      statisticsJson.put("externalContentId", externalContentId);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String fileName = String.format("daily_popular_%s_%s_%s_%s.json",
          platformType, externalContentId, categoryName, timestamp);
      String s3Path = String.format("processed_data/%s/daily_popular_content/%s/%s",platformType, dateFolder, fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 인기콘텐츠 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 인기콘텐츠 저장 실패: platform={}, externalContentId={}",
          platformType, row.getAs("externalContentId"), e);
    }
  }

  private void saveDailyPopularContent(String platform, Integer categoryTypeId, Row row, LocalDate targetDate,Integer contentId,Integer contentRank) {
    try {
      // 1. MySQL INSERT/UPDATE 쿼리
      String sql = "INSERT INTO daily_popular_content " +
          "(platform_type_id, content_id, category_type_id, content_rank, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "content_rank = VALUES(content_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      // 2. 파라미터 바인딩
      jdbcTemplate.update(sql,
          platform,
          contentId,
          categoryTypeId,
          contentRank,
          targetDate,
          LocalDateTime.now()
      );

    } catch (Exception e) {
      log.error("DailyPopularContent 저장 실패", e);
    }
  }

}