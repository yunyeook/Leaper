package com.ssafy.spark.domain.insight.service;

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
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkPopularInfluencerService extends SparkBaseService {

  /**
   * DailyPopularInfluencer 생성
   * 
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate   통계를 생성할 기준 날짜
   */
  public void generateDailyPopularInfluencer(String platformType, LocalDate targetDate, Dataset<Row> contentData,
      Dataset<Row> accountDataBase) {
    try {
      // 1. 최근 30일 이내에 활동(포스팅)이 있는 계정 닉네임 추출
      Dataset<Row> activeAccountNicknames = contentData
          .filter(col("publishedAt").isNotNull())
          .filter(to_date(col("publishedAt")).gt(lit(targetDate.minusDays(30).toString())))
          .select("accountNickname")
          .distinct();

      // 2. 계정 데이터 필터링 (활동 중인 계정만)
      Dataset<Row> accountData = accountDataBase
          .join(activeAccountNicknames, "accountNickname")
          .select("accountNickname", "categoryName", "followersCount");

      // 3. 카테고리별로 Top10 추출
      Dataset<Row> top10 = accountData
          .withColumn("influencerRank", row_number().over(
              Window.partitionBy("categoryName")
                  .orderBy(col("followersCount").desc())))
          .filter(col("influencerRank").leq(10));

      // 3. 결과 수집
      List<Row> results = top10.collectAsList();

      log.info("[{}] Top10 인플루언서 개수: {}", platformType, results.size());

      // 🔥 Batch Insert를 위한 데이터 수집
      List<PopularInfluencerBatch> batchData = new ArrayList<>();

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType, accountNickname);

        if (platformAccountId == null) {
          log.warn("PlatformAccount not found, skipping: platform={}, nickname={}", platformType, accountNickname);
          continue;
        }

        String categoryName = row.getAs("categoryName");
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer influencerRank = row.getAs("influencerRank");
        Long followersCount = row.getAs("followersCount");
        Integer influencerId = getInfluencerIdByPlatformAccount(platformAccountId);

        if (influencerId == null) {
          log.warn("InfluencerId not found, skipping: platformAccountId={}", platformAccountId);
          continue;
        }

        // Batch 데이터 수집
        batchData.add(new PopularInfluencerBatch(
            platformType.toUpperCase(),
            influencerId,
            categoryTypeId,
            influencerRank,
            targetDate));

        // S3에 저장
        savePopularInfluencerToS3(platformType, categoryName, row, targetDate, platformAccountId, influencerRank,
            accountNickname, followersCount, influencerId);

        // 1000개씩 끊어서 저장
        if (batchData.size() >= 1000) {
          saveDailyPopularInfluencerBatch(batchData);
          batchData.clear();
        }
      }

      // 남은 데이터 저장
      if (!batchData.isEmpty()) {
        saveDailyPopularInfluencerBatch(batchData);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyPopularInfluencer 생성 실패", e);
    }
  }

  /**
   * Batch Insert를 위한 내부 클래스
   */
  private static class PopularInfluencerBatch {
    String platformType;
    Integer influencerId;
    Integer categoryTypeId;
    Integer influencerRank;
    LocalDate targetDate;

    public PopularInfluencerBatch(String platformType, Integer influencerId, Integer categoryTypeId,
        Integer influencerRank, LocalDate targetDate) {
      this.platformType = platformType;
      this.influencerId = influencerId;
      this.categoryTypeId = categoryTypeId;
      this.influencerRank = influencerRank;
      this.targetDate = targetDate;
    }
  }

  private void savePopularInfluencerToS3(String platformType, String categoryName, Row row, LocalDate targetDate,
      Integer platformAccountId, Integer influencerRank, String accountNickname, Long followersCount,
      Integer influencerId) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("influencerId", influencerId);
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("accountNickname", accountNickname);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("influencerRank", influencerRank);
      statisticsJson.put("followersCount", followersCount);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로 (✅ 통일된 구조)
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_popular_influencer_%s_%s_%s.json", accountNickname, categoryName,
          timestamp);
      String s3Path = String.format("processed_data/json/%s/daily_popular_influencer/%s/%s", platformType, dateFolder,
          fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 인기인플루언서 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 인기인플루언서 저장 실패: platformType={}, accountNickname={}",
          platformType, accountNickname, e);
    }
  }

  /**
   * 🔥 Batch Insert로 대량 데이터 한 번에 저장
   */
  private void saveDailyPopularInfluencerBatch(List<PopularInfluencerBatch> batchData) {
    if (batchData.isEmpty()) {
      log.warn("저장할 데이터가 없습니다.");
      return;
    }

    try {
      String sql = "INSERT INTO daily_popular_influencer " +
          "(platform_type_id, influencer_id, category_type_id, influencer_rank, " +
          "snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "influencer_rank = VALUES(influencer_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      LocalDateTime now = LocalDateTime.now();

      jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
        @Override
        public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
          PopularInfluencerBatch data = batchData.get(i);
          ps.setString(1, data.platformType);
          ps.setInt(2, data.influencerId);
          ps.setInt(3, data.categoryTypeId);
          ps.setInt(4, data.influencerRank);
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
      throw new RuntimeException("DailyPopularInfluencer Batch 저장 실패", e);
    }
  }

  /**
   * @deprecated 개별 저장 방식 (Batch Insert로 대체됨)
   */
  @Deprecated
  private void saveDailyPopularInfluencer(String platformType, Integer categoryTypeId, Row row, LocalDate targetDate,
      Integer influencerRank, Integer influencerId) {
    try {

      // 1. MySQL INSERT/UPDATE 쿼리
      String sql = "INSERT INTO daily_popular_influencer " +
          "(platform_type_id, influencer_id, category_type_id, influencer_rank, " +
          "snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "influencer_rank = VALUES(influencer_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      // 2. 파라미터 바인딩
      jdbcTemplate.update(sql,
          platformType.toUpperCase(),
          influencerId,
          categoryTypeId,
          influencerRank,
          targetDate,
          LocalDateTime.now());

    } catch (Exception e) {
      log.error("DailyPopularInfluencer 저장 실패", e);
    }
  }
}