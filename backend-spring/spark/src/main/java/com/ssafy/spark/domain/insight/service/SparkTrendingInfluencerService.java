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
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkTrendingInfluencerService extends SparkBaseService {

  /**
   * DailyTrendingInfluencer 생성
   * 
   * @param platformType     플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate       통계를 생성할 기준 날짜
   * @param todayAccountData 오늘의 계정 데이터 (캐시됨)
   */
  public void generateDailyTrendingInfluencer(String platformType, LocalDate targetDate,
      Dataset<Row> todayAccountData) {
    try {
      // 1. 오늘 데이터 (파라미터로 받음)
      Dataset<Row> todayData = todayAccountData
          .select("accountNickname", "categoryName", "followersCount");

      // 2. 어제 팔로워 수만 조회
      Dataset<Row> yesterdayFollowers = readS3AccountData(platformType, targetDate.minusDays(1))
          .select("accountNickname", "followersCount")
          .withColumnRenamed("followersCount", "yesterdayFollowers");

      // 3. 계정명으로 left 조인
      Dataset<Row> joined = todayData.join(
          yesterdayFollowers,
          new String[] { "accountNickname" },
          "left");

      // 4. 증감량 계산 (어제 데이터가 없으면 증감량을 0으로)
      Dataset<Row> trendingTop10 = joined
          .withColumn("deltaFollowers",
              when(col("yesterdayFollowers").isNull(), lit(0L))
                  .otherwise(col("followersCount").minus(col("yesterdayFollowers"))))
          .withColumn("influencerRank",
              row_number().over(
                  Window.partitionBy("categoryName")
                      .orderBy(col("deltaFollowers").desc())))
          .filter(col("influencerRank").leq(10));
      // 5. 결과 수집
      List<Row> results = trendingTop10.collectAsList();

      log.info("[{}] DailyTrendingInfluencer Top10 개수: {}", platformType, results.size());

      List<TrendingInfluencerBatch> batchData = new java.util.ArrayList<>();

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);

        if (platformAccountId == null) {
          log.warn("PlatformAccount not found, skipping: platform={}, nickname={}", platformType, accountNickname);
          continue;
        }

        String categoryName = row.getAs("categoryName");
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer influencerRank = row.getAs("influencerRank");
        Long followersCount = row.getAs("followersCount");
        Long deltaFollowers = row.getAs("deltaFollowers");
        Integer influencerId = getInfluencerIdByPlatformAccount(platformAccountId);

        if (influencerId == null) {
          log.warn("InfluencerId not found, skipping: platformAccountId={}", platformAccountId);
          continue;
        }

        // 1) Batch 데이터 수집
        batchData.add(new TrendingInfluencerBatch(
            platformType.toUpperCase(),
            influencerId,
            categoryTypeId,
            influencerRank,
            targetDate));

        // 1000개씩 끊어서 저장
        if (batchData.size() >= 1000) {
          saveDailyTrendingInfluencerBatch(batchData);
          batchData.clear();
        }

        // 2) S3 저장
        saveTrendingInfluencerToS3(platformType, categoryName, targetDate, platformAccountId, influencerId,
            influencerRank, accountNickname, followersCount, deltaFollowers);
      }

      // 남은 데이터 저장
      if (!batchData.isEmpty()) {
        saveDailyTrendingInfluencerBatch(batchData);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyTrendingInfluencer 생성 실패", e);
    }
  }

  /**
   * Batch Insert DTO
   */
  @lombok.AllArgsConstructor
  private static class TrendingInfluencerBatch {
    String platformTypeId;
    Integer influencerId;
    Integer categoryTypeId;
    Integer influencerRank;
    LocalDate snapshotDate;
  }

  /**
   * JDBC Batch Update
   */
  private void saveDailyTrendingInfluencerBatch(List<TrendingInfluencerBatch> batchData) {
    if (batchData.isEmpty())
      return;

    String sql = "INSERT INTO daily_trending_influencer " +
        "(platform_type_id, influencer_id, category_type_id, influencer_rank, snapshot_date, created_at) " +
        "VALUES (?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE " +
        "influencer_rank = VALUES(influencer_rank), " +
        "snapshot_date = VALUES(snapshot_date), " +
        "created_at = VALUES(created_at)";

    jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
      @Override
      public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
        TrendingInfluencerBatch dto = batchData.get(i);
        ps.setString(1, dto.platformTypeId);
        ps.setInt(2, dto.influencerId);
        ps.setInt(3, dto.categoryTypeId);
        ps.setInt(4, dto.influencerRank);
        ps.setObject(5, dto.snapshotDate);
        ps.setObject(6, LocalDateTime.now());
      }

      @Override
      public int getBatchSize() {
        return batchData.size();
      }
    });
  }

  private void saveTrendingInfluencerToS3(String platformType, String categoryName, LocalDate targetDate,
      Integer platformAccountId, Integer influencerId, Integer influencerRank,
      String accountNickname, Long followersCount, Long deltaFollowers) {
    try {
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("influencerId", influencerId);
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("accountNickname", accountNickname);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("influencerRank", influencerRank);
      statisticsJson.put("followersCount", followersCount);
      statisticsJson.put("deltaFollowers", deltaFollowers);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticsJson);

      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_trending_influencer_%s_%s.json", accountNickname, timestamp);
      String s3Path = String.format("processed_data/json/%s/daily_trending_influencer/%s/%s",
          platformType, dateFolder, fileName);

      uploadFile(s3Path, jsonData.getBytes(), "application/json");
      log.info("S3 급상승 인플루언서 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 급상승 인플루언서 저장 실패: platformType={}, accountNickname={}", platformType, accountNickname, e);
    }
  }
}
