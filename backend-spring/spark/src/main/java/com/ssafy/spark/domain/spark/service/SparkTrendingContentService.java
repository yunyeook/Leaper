package com.ssafy.spark.domain.spark.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.Window;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkTrendingContentService extends SparkBaseService {

  /**
   * DailyTrendingContent 생성
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate 통계를 생성할 기준 날짜
   */
  public void generateDailyTrendingContent(String platformType, LocalDate targetDate) {
    try {
      // 1. 오늘 콘텐츠 데이터
      Dataset<Row> todayContentData = readS3ContentDataByDate(platformType, targetDate)
          .select("externalContentId", "accountNickname", "viewsCount");

      // 2. 어제 조회수만 조회
      Dataset<Row> yesterdayViews = readS3ContentDataByDate(platformType, targetDate.minusDays(1))
          .select("externalContentId", "viewsCount")
          .withColumnRenamed("viewsCount", "yesterdayViews");

      // 3. 계정 데이터 (카테고리 정보)
      Dataset<Row> accountData = readS3AccountData(platformType, targetDate)
          .select("accountNickname", "categoryName");

      // 4. 콘텐츠와 계정 조인
      Dataset<Row> contentWithCategory = todayContentData
          .join(accountData, "accountNickname")
          .filter(col("categoryName").isNotNull());

      // 5. 어제 조회수와 조인
      Dataset<Row> joined = contentWithCategory
          .join(yesterdayViews, "externalContentId");

      // 6. 증감량 계산 및 카테고리별 Top10 처리
      Dataset<Row> trendingTop10 = joined
          .withColumn("deltaViews",
              col("viewsCount").minus(col("yesterdayViews")))
          .withColumn("contentRank",
              row_number().over(
                  Window.partitionBy("categoryName")
                      .orderBy(col("deltaViews").desc())
              ))
          .filter(col("contentRank").leq(10));

      // 7. 결과 수집
      List<Row> results = trendingTop10.collectAsList();

      log.info("[{}] DailyTrendingContent Top10 개수: {}", platformType, results.size());

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        String accountNickname = row.getAs("accountNickname");
        String categoryName = row.getAs("categoryName");
        Integer contentRank = row.getAs("contentRank");
        Long viewsCount = row.getAs("viewsCount");
        Long deltaViews = row.getAs("deltaViews");

        Integer contentId = getContentId(platformType, externalContentId);
        Integer categoryTypeId = getCategoryTypeId(categoryName);

        // 1) MySQL 저장
        saveDailyTrendingContent(platformType, categoryTypeId, targetDate, contentId, contentRank, deltaViews);

        // 2) S3 저장
        saveTrendingContentToS3(platformType, categoryName, targetDate, contentId, contentRank,
            externalContentId, accountNickname, viewsCount, deltaViews);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyTrendingContent 생성 실패", e);
    }
  }

  private void saveDailyTrendingContent(String platformType, Integer categoryTypeId, LocalDate targetDate,
      Integer contentId, Integer contentRank, Long deltaViews) {
    try {
      String sql = "INSERT INTO daily_trending_content " +
          "(platform_type_id, content_id, category_type_id, content_rank, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "content_rank = VALUES(content_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      jdbcTemplate.update(sql,
          platformType.toUpperCase(),
          contentId,
          categoryTypeId,
          contentRank,
          targetDate,
          LocalDateTime.now()
      );

    } catch (Exception e) {
      log.error("DailyTrendingContent 저장 실패", e);
    }
  }

  private void saveTrendingContentToS3(String platformType, String categoryName, LocalDate targetDate,
      Integer contentId, Integer contentRank, String externalContentId,
      String accountNickname, Long viewsCount, Long deltaViews) {
    try {
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("contentId", contentId);
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("externalContentId", externalContentId);
      statisticsJson.put("accountNickname", accountNickname);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("contentRank", contentRank);
      statisticsJson.put("viewsCount", viewsCount);
      statisticsJson.put("deltaViews", deltaViews);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticsJson);

      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_trending_content_%s_%s.json", externalContentId, timestamp);
      String s3Path = String.format("processed_data/%s/daily_trending_content/%s/%s",
          platformType, dateFolder, fileName);

      uploadFile(s3Path, jsonData.getBytes(), "application/json");
      log.info("S3 급상승 콘텐츠 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 급상승 콘텐츠 저장 실패: platformType={}, externalContentId={}", platformType, externalContentId, e);
    }
  }
}
