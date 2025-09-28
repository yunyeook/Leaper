package com.ssafy.spark.domain.analysis.service;

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
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate   통계를 생성할 기준 날짜
   */
  public void generateDailyTrendingInfluencer(String platformType, LocalDate targetDate) {
    try {
      // 1. 오늘 데이터 (기본 정보 포함)
      Dataset<Row> todayData = readS3AccountData(platformType, targetDate)
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

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);
        String categoryName = row.getAs("categoryName");
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer influencerRank = row.getAs("influencerRank");
        Long followersCount = row.getAs("followersCount");
        Long deltaFollowers = row.getAs("deltaFollowers");
        Integer influencerId = getInfluencerIdByPlatformAccount(platformAccountId);

        // 1) MySQL 저장
        saveDailyTrendingInfluencer(platformType, categoryTypeId, targetDate, influencerId, influencerRank,
            deltaFollowers);

        // 2) S3 저장
        saveTrendingInfluencerToS3(platformType, categoryName, targetDate, platformAccountId, influencerId,
            influencerRank, accountNickname, followersCount, deltaFollowers);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyTrendingInfluencer 생성 실패", e);
    }
  }

  private void saveDailyTrendingInfluencer(String platformType, Integer categoryTypeId, LocalDate targetDate,
      Integer influencerId,
      Integer influencerRank, Long deltaFollowers) {
    try {
      String sql = "INSERT INTO daily_trending_influencer " +
          "(platform_type_id, influencer_id, category_type_id, influencer_rank, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "influencer_rank = VALUES(influencer_rank), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      jdbcTemplate.update(sql,
          platformType.toUpperCase(),
          influencerId,
          categoryTypeId,
          influencerRank,
          targetDate,
          LocalDateTime.now());

    } catch (Exception e) {
      log.error("DailyTrendingInfluencer 저장 실패", e);
    }
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
      String s3Path = String.format("processed_data/%s/daily_trending_influencer/%s/%s",
          platformType, dateFolder, fileName);

      uploadFile(s3Path, jsonData.getBytes(), "application/json");
      log.info("S3 급상승 인플루언서 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 급상승 인플루언서 저장 실패: platformType={}, accountNickname={}", platformType, accountNickname, e);
    }
  }
}
