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
public class SparkPopularInfluencerService extends SparkBaseService {

  /**
   * DailyPopularInfluencer 생성
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate 통계를 생성할 기준 날짜
   */
  public void generateDailyPopularInfluencer(String platformType, LocalDate targetDate) {
    try {
      // 1. 계정 데이터 읽기
      Dataset<Row> accountData = readS3AccountData(platformType,targetDate)
          .select("accountNickname", "categoryName", "followersCount");

      // 2. 카테고리별로 Top10 추출
      Dataset<Row> top10 = accountData
          .withColumn("influencerRank", row_number().over(
              Window.partitionBy("categoryName")
                  .orderBy(col("followersCount").desc())
          ))
          .filter(col("influencerRank").leq(10));

       // 3. 결과 수집
      List<Row> results = top10.collectAsList();


      log.info("[{}] Top10 인플루언서 개수: {}", platformType, results.size());

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType, accountNickname);
        String categoryName = row.getAs("categoryName");
        Integer categoryTypeId = getCategoryTypeId(categoryName);
        Integer influencerRank = row.getAs("influencerRank");
        Long followersCount = row.getAs("followersCount");
        Integer influencerId = getInfluencerIdByPlatformAccount(platformAccountId);


        // 1) MySQL에 저장
        saveDailyPopularInfluencer(platformType, categoryTypeId, row, targetDate, influencerRank, influencerId);

        // 2) S3에도 저장
        savePopularInfluencerToS3(platformType, categoryName, row, targetDate, platformAccountId, influencerRank, accountNickname,followersCount,influencerId);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyPopularInfluencer 생성 실패", e);
    }
  }

  private void savePopularInfluencerToS3(String platformType, String categoryName, Row row, LocalDate targetDate,
      Integer platformAccountId, Integer influencerRank, String accountNickname, Long followersCount, Integer influencerId) {
    try {

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("influencerId", influencerId);
      statisticsJson.put("platformType", platformType);
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("accountNickname", accountNickname);
      statisticsJson.put("categoryName", categoryName);
      statisticsJson.put("influencerRank", influencerRank);
      statisticsJson.put("followersCount", followersCount);
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("createdAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String fileName = String.format("daily_popular_influencer_%s_%s_%s.json", accountNickname, categoryName, timestamp);
      String s3Path = String.format("processed_data/%s/daily_popular_influencer/%s/%s", platformType, dateFolder, fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 인기인플루언서 저장 완료: {}", s3Path);

    } catch (Exception e) {
      log.error("S3 인기인플루언서 저장 실패: platformType={}, accountNickname={}",
          platformType, accountNickname, e);
    }
  }

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
          platformType,
          influencerId,
          categoryTypeId,
          influencerRank,
          targetDate,
          LocalDateTime.now()
      );

    } catch (Exception e) {
      log.error("DailyPopularInfluencer 저장 실패", e);
    }
  }
}