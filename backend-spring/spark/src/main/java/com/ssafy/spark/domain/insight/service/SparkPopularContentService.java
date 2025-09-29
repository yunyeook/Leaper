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
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkPopularContentService extends SparkBaseService {

  /**
   * DailyPopularContent 생성
   * 
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate   통계를 생성할 기준 날짜
   */
  public void generateDailyPopularContent(String platformType, LocalDate targetDate, Dataset<Row> contentData,
      Dataset<Row> accountDataBase) {
    try {
      // 2. 계정 데이터 읽기
      Dataset<Row> accountData = accountDataBase
          .select("accountNickname", "categoryName");

      // 3. 필터링 및 조인 (최근 30일 이내 콘텐츠 + 카테고리 매칭)
      Dataset<Row> filteredContent = contentData
          .filter(col("publishedAt").isNotNull())
          .filter(to_date(col("publishedAt")).gt(lit(targetDate.minusDays(30).toString())));

      Dataset<Row> joined = filteredContent
          .join(accountData, "accountNickname") // 공통 키로 조인
          .filter(col("categoryName").isNotNull());

      // 4. 카테고리별 조회수 기준 Top10 추출
      Dataset<Row> top10 = joined
          .withColumn("contentRank", row_number().over(
              org.apache.spark.sql.expressions.Window
                  .partitionBy("categoryName") // 카테고리별 그룹
                  .orderBy(col("viewsCount").desc())))
          .filter(col("contentRank").leq(10));

      // 5. 결과 수집
      List<Row> results = top10.collectAsList();
      log.info("[{}] 전체 카테고리 Top10 콘텐츠 개수: {}", platformType, results.size());

      // 🔥 Batch Insert를 위한 데이터 수집
      List<PopularContentBatch> batchData = new ArrayList<>();

      for (Row row : results) {
        String externalContentId = row.getAs("externalContentId");
        String categoryName = row.getAs("categoryName");

        Integer contentId = getContentId(platformType.toUpperCase(), externalContentId);

        if (contentId == null) {
          log.warn("Content not found, skipping: platform={}, externalContentId={}", platformType, externalContentId);
          continue;
        }

        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer contentRank = row.getAs("contentRank");

        // Batch 데이터 수집
        batchData.add(new PopularContentBatch(
            platformType.toUpperCase(),
            contentId,
            categoryTypeId,
            contentRank,
            targetDate));

        // S3 저장
        savePopularContentToS3(platformType, categoryName, row, targetDate, contentId, contentRank, externalContentId);

        // 1000개씩 끊어서 저장
        if (batchData.size() >= 1000) {
          saveDailyPopularContentBatch(batchData);
          batchData.clear();
        }
      }

      // 남은 데이터 저장
      if (!batchData.isEmpty()) {
        saveDailyPopularContentBatch(batchData);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyPopularContent 생성 실패", e);
    }
  }

  /**
   * Batch Insert를 위한 내부 클래스
   */
  private static class PopularContentBatch {
    String platformType;
    Integer contentId;
    Integer categoryTypeId;
    Integer contentRank;
    LocalDate targetDate;

    public PopularContentBatch(String platformType, Integer contentId, Integer categoryTypeId,
        Integer contentRank, LocalDate targetDate) {
      this.platformType = platformType;
      this.contentId = contentId;
      this.categoryTypeId = categoryTypeId;
      this.contentRank = contentRank;
      this.targetDate = targetDate;
    }
  }

  private void savePopularContentToS3(String platformType, String categoryName, Row row, LocalDate targetDate,
      Integer contentId, Integer contentRank, String externalContentId) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("contentId", contentId);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("contentRank", contentRank);
      statisticsJson.put("externalContentId", externalContentId);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로 (✅ 통일된 구조)
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_popular_content_%s_%s_%s.json", externalContentId, categoryName,
          timestamp);
      String s3Path = String.format("processed_data/json/%s/daily_popular_content/%s/%s", platformType, dateFolder,
          fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 인기콘텐츠 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 인기콘텐츠 저장 실패: platform={}, externalContentId={}",
          platformType, row.getAs("externalContentId"), e);
    }
  }

  /**
   * 🔥 Batch Insert로 대량 데이터 한 번에 저장
   */
  private void saveDailyPopularContentBatch(List<PopularContentBatch> batchData) {
    if (batchData.isEmpty()) {
      log.warn("저장할 데이터가 없습니다.");
      return;
    }

    try {
      String sql = "INSERT INTO daily_popular_content " +
          "(platform_type_id, content_id, category_type_id, content_rank, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "content_rank = VALUES(content_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      LocalDateTime now = LocalDateTime.now();

      jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
        @Override
        public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
          PopularContentBatch data = batchData.get(i);
          ps.setString(1, data.platformType);
          ps.setInt(2, data.contentId);
          ps.setInt(3, data.categoryTypeId);
          ps.setInt(4, data.contentRank);
          ps.setObject(5, data.targetDate);
          ps.setObject(6, now);
        }

        @Override
        public int getBatchSize() {
          return batchData.size();
        }
      });

      log.info("✅ Batch Insert 완료: {} 건 저장", batchData.size());

    } catch (Exception e) {
      log.error("❌ Batch Insert 실패: {} 건", batchData.size(), e);
      throw new RuntimeException("DailyPopularContent Batch 저장 실패", e);
    }
  }

  /**
   * @deprecated 개별 저장 방식 (Batch Insert로 대체됨)
   */
  @Deprecated
  private void saveDailyPopularContent(String platformType, Integer categoryTypeId, Row row, LocalDate targetDate,
      Integer contentId, Integer contentRank) {
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
          platformType.toUpperCase(),
          contentId,
          categoryTypeId,
          contentRank,
          targetDate,
          LocalDateTime.now());

    } catch (Exception e) {
      log.error("DailyPopularContent 저장 실패", e);
    }
  }

}