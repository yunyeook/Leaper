package com.ssafy.spark.domain.spark.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkAccountInsightService extends SparkBaseService {

  /**
   * DailyAccountInsight 생성
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate 통계를 생성할 기준 날짜 (예: 2024-01-15)
   */
  public void generateDailyAccountInsight(String platformType,LocalDate targetDate) {
    try {
      // 1. 콘텐츠 데이터 읽기 (조회수, 좋아요, 댓글 집계용)
      Dataset<Row> contentData = readS3ContentDataByDate(platformType,targetDate);

      // 2. 계정 프로필 데이터 읽기 (팔로워 수 정보용)
      Dataset<Row> accountData = readS3AccountData(platformType);

      // 3. 콘텐츠별 통계 집계 (계정별로 그룹핑)
      Dataset<Row> contentStatistics = contentData
          .filter(col("externalAccountId").isNotNull())
          .groupBy("externalAccountId")
          .agg(
              // 게시물들의 조회수 합계
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              // 게시물들의 좋아요 합계
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              // 게시물들의 댓글 합계
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              // 해당 계정의 게시물 개수
              count("*").alias("totalContents")
          );

      // 4. 계정 프로필 정보에서 팔로워 수 추출
      Dataset<Row> accountFollowers = accountData
          .filter(col("externalAccountId").isNotNull())
          .select("externalAccountId", "followersCount")
          .withColumnRenamed("followersCount", "totalFollowers");

      // 5. 콘텐츠 통계 + 계정 정보 조인
      Dataset<Row> finalStatistics = contentStatistics
          .join(accountFollowers, "externalAccountId")  // externalAccountId로 조인
          .select("externalAccountId", "totalViews", "totalLikes",
              "totalComments", "totalContents", "totalFollowers");

      // 6. 결과를 MySQL에 저장
      List<Row> results = finalStatistics.collectAsList();
log.info("계정 몇개?");
      log.info(String.valueOf(results.size()));

      for (Row row : results) {
        // 1) MySQL에 저장
        saveDailyAccountInsight(platformType, row, targetDate);

        // 2) S3에도 저장
        saveStatisticsToS3(platformType, row, targetDate);
      }
    } catch (Exception e) {
        throw new RuntimeException("DailyAccountInsight 생성 실패", e);
    }
  }

  private void saveStatisticsToS3(String platform, Row row, LocalDate targetDate) {
    try {
      String externalAccountId = row.getAs("externalAccountId");

      // 통계 결과를 JSON으로 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("externalAccountId", externalAccountId);
      statisticsJson.put("platform", platform);
      statisticsJson.put("totalViews", row.getAs("totalViews").toString());
      statisticsJson.put("totalLikes", row.getAs("totalLikes").toString());
      statisticsJson.put("totalComments", row.getAs("totalComments").toString());

      // Long → Integer 안전 변환
      Long totalContents = row.getAs("totalContents");
      Long totalFollowers = row.getAs("totalFollowers");

      statisticsJson.put("totalContents", totalContents != null ? totalContents.intValue() : 0);
      statisticsJson.put("totalFollowers", totalFollowers != null ? totalFollowers.intValue() : 0);

      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("processedAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(statisticsJson);

      // S3 저장 경로
      // 날짜 폴더 (예: 2025/09/16)
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String fileName = String.format("daily_stats_%s_%s_%s.json",
          platform, externalAccountId, timestamp);
      String s3Path = String.format("processed_data/%s/daily_account_insight/%s/%s",
          platform, dateFolder, fileName);

      // S3에 저장
      uploadFile(s3Path, jsonData.getBytes(), "application/json");

      log.info("S3 통계 저장 완료: {}", s3Path);  // debug → info 변경

    } catch (Exception e) {
      log.error("S3 통계 저장 실패: platform={}, externalAccountId={}",
          platform, row.getAs("externalAccountId"), e);
    }
  }

  private void saveDailyAccountInsight(String platform, Row row, LocalDate targetDate) {
    try {
      // 1. externalAccountId → platform_account_id 변환
      String externalAccountId = row.getAs("externalAccountId");
      Integer platformAccountId = getPlatformAccountId(platform, externalAccountId);

      if (platformAccountId == null) {
        log.warn("PlatformAccount not found: platform={}, externalAccountId={}",
            platform, externalAccountId);
        return;
      }

      // 2. DDL 타입에 맞게 데이터 변환 (BigInteger 유지)
      BigInteger totalViews = getBigIntegerValue(row, "totalViews");
      BigInteger totalLikes = getBigIntegerValue(row, "totalLikes");
      BigInteger totalComments = getBigIntegerValue(row, "totalComments");

      // INT UNSIGNED는 Integer 사용
      Integer totalFollowers = getIntegerValue(row, "totalFollowers");
      Integer totalContents = getIntegerValue(row, "totalContents");

      // 3. like_score 계산
      Double likeScore = calculateLikeScore(totalViews, totalLikes, totalComments);

      // 4. MySQL INSERT/UPDATE 쿼리
      String sql = "INSERT INTO daily_account_insight " +
          "(platform_account_id, total_views, total_followers, total_contents, " +
          " total_likes, total_comments, like_score, snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "total_views = VALUES(total_views), " +
          "total_followers = VALUES(total_followers), " +
          "total_contents = VALUES(total_contents), " +
          "total_likes = VALUES(total_likes), " +
          "total_comments = VALUES(total_comments), " +
          "like_score = VALUES(like_score), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      // 5. 파라미터 바인딩
      jdbcTemplate.update(sql,
          platformAccountId,      // platform_account_id (INT UNSIGNED)
          totalViews,            // total_views (BIGINT UNSIGNED) → BigInteger
          totalFollowers,        // total_followers (INT UNSIGNED) → Integer
          totalContents,         // total_contents (INT UNSIGNED) → Integer
          totalLikes,            // total_likes (BIGINT UNSIGNED) → BigInteger
          totalComments,         // total_comments (BIGINT UNSIGNED) → BigInteger
          likeScore,             // like_score (DECIMAL(5,2)) → BigInteger (근데 이건 문제...)
          targetDate,            // snapshot_date (DATE)
          LocalDateTime.now()    // created_at (DATETIME)
      );

    } catch (Exception e) {
      log.error("DailyAccountInsight 저장 실패", e);
    }
  }

  private Double calculateLikeScore(BigInteger totalViews, BigInteger totalLikes, BigInteger totalComments) {
    if (totalViews.equals(BigInteger.ZERO)) return 0.0;

    // BigInteger를 double로 변환해서 계산
    double views = totalViews.doubleValue();
    double likes = totalLikes.doubleValue();
    double comments = totalComments.doubleValue();

    //TODO: 호감도 점수 계산 수정하기
    // 일단 단순계산으로 구현함

    double engagementRate = (likes + comments) / views * 100;
    double score = Math.min(100.0, Math.max(-100.0, engagementRate * 10 - 50));

    return Math.round(score * 100.0) / 100.0;
  }
}