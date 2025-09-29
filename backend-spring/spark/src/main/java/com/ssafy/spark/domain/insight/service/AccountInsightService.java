package com.ssafy.spark.domain.insight.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInsightService extends SparkBaseService {

  /**
   * DailyAccountInsight 생성
   * 
   * @param platformType 플랫폼 타입 (예: "youtube", "instagram", "naver_blog")
   * @param targetDate   통계를 생성할 기준 날짜 (예: 2024-01-15)
   */
  public void generateDailyAccountInsight(String platformType, LocalDate targetDate, Dataset<Row> contentData,
      Dataset<Row> accountData) {
    try {

      // 3. 콘텐츠별 통계 집계 (계정별로 accountNickname 그룹핑)
      Dataset<Row> contentStatistics = contentData
          .filter(col("accountNickname").isNotNull())
          .groupBy("accountNickname")
          .agg(
              // 게시물들의 조회수 합계(Long 타입으로 자동 저장)
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              // 게시물들의 좋아요 합계
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              // 게시물들의 댓글 합계
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              // 해당 계정의 게시물 개수
              count("*").alias("totalContents"));

      // 4. 계정 프로필 정보에서 팔로워 수 추출
      Dataset<Row> accountFollowers = accountData
          .filter(col("accountNickname").isNotNull())
          .select("accountNickname", "followersCount")
          .withColumnRenamed("followersCount", "totalFollowers");

      // 5. 콘텐츠 통계 + 계정 정보 조인
      Dataset<Row> joinedData = contentStatistics
          .join(accountFollowers, "accountNickname")
          .select("accountNickname", "totalViews", "totalLikes",
              "totalComments", "totalContents", "totalFollowers");

      // 6. 결과 저장
      List<Row> results = joinedData.collectAsList();
      log.info("계정 몇개?");
      log.info(String.valueOf(results.size()));

      // 🔥 Batch Insert를 위한 데이터 수집
      List<AccountInsightBatch> batchData = new ArrayList<>();

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);

        if (platformAccountId == null) {
          log.warn("PlatformAccount not found: platformType={}, accountNickname={}",
              platformType, accountNickname);
          continue;
        }

        // 데이터 변환
        BigInteger totalViews = getBigIntegerValue(row, "totalViews");
        BigInteger totalLikes = getBigIntegerValue(row, "totalLikes");
        BigInteger totalComments = getBigIntegerValue(row, "totalComments");
        Integer totalFollowers = getIntegerValue(row, "totalFollowers");
        Integer totalContents = getIntegerValue(row, "totalContents");
        Double likeScore = calculateLikeScore(totalViews, totalLikes, totalComments, totalFollowers);

        // Batch 데이터 수집
        batchData.add(new AccountInsightBatch(
            platformAccountId,
            totalViews,
            totalFollowers,
            totalContents,
            totalLikes,
            totalComments,
            likeScore,
            targetDate));

        // 1000개씩 끊어서 저장 (메모리 절약)
        if (batchData.size() >= 1000) {
          saveDailyAccountInsightBatch(batchData);
          batchData.clear();
        }

        // S3에 저장
        saveStatisticsToS3(platformType, row, targetDate, platformAccountId, accountNickname);
      }

      // 남은 데이터 저장
      if (!batchData.isEmpty()) {
        saveDailyAccountInsightBatch(batchData);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyAccountInsight 생성 실패", e);
    }
  }

  /**
   * Batch Insert를 위한 내부 클래스
   */
  private static class AccountInsightBatch {
    Integer platformAccountId;
    BigInteger totalViews;
    Integer totalFollowers;
    Integer totalContents;
    BigInteger totalLikes;
    BigInteger totalComments;
    Double likeScore;
    LocalDate targetDate;

    public AccountInsightBatch(Integer platformAccountId, BigInteger totalViews, Integer totalFollowers,
        Integer totalContents, BigInteger totalLikes, BigInteger totalComments,
        Double likeScore, LocalDate targetDate) {
      this.platformAccountId = platformAccountId;
      this.totalViews = totalViews;
      this.totalFollowers = totalFollowers;
      this.totalContents = totalContents;
      this.totalLikes = totalLikes;
      this.totalComments = totalComments;
      this.likeScore = likeScore;
      this.targetDate = targetDate;
    }
  }

  private void saveStatisticsToS3(String platformType, Row row, LocalDate targetDate,
      Integer platformAccountId, String accountNickname) {
    try {
      // 1️⃣ JSON 객체 생성
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("totalViews", row.getAs("totalViews").toString());
      statisticsJson.put("totalLikes", row.getAs("totalLikes").toString());
      statisticsJson.put("totalComments", row.getAs("totalComments").toString());
      statisticsJson.put("totalContents",
          row.getAs("totalContents") != null ? row.getAs("totalContents").toString() : "0");
      statisticsJson.put("totalFollowers",
          row.getAs("totalFollowers") != null ? row.getAs("totalFollowers").toString() : "0");
      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("processedAt", LocalDateTime.now().toString());

      // 2️⃣ JSON 직렬화
      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticsJson);

      // 3️⃣ 파일 경로 구성
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileBaseName = String.format("daily_account_insight_%s_%s", accountNickname, timestamp);

      String jsonKey = String.format("processed_data/json/%s/daily_account_insight/%s/%s.json",
          platformType, dateFolder, fileBaseName);

      // 4️⃣ JSON 업로드
      uploadFile(jsonKey, jsonData.getBytes(StandardCharsets.UTF_8), "application/json");
      log.info("S3 통계(JSON) 저장 완료: {}", jsonKey);

    } catch (Exception e) {
      log.error("S3 통계 저장 실패: platformType={}, accountNickname={}",
          platformType, accountNickname, e);
      throw new RuntimeException("S3 통계 저장 실패", e);
    }
  }

  /**
   * 🔥 Batch Insert로 대량 데이터 한 번에 저장
   */
  private void saveDailyAccountInsightBatch(List<AccountInsightBatch> batchData) {
    if (batchData.isEmpty()) {
      log.warn("저장할 데이터가 없습니다.");
      return;
    }

    try {
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

      LocalDateTime now = LocalDateTime.now();

      jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
        @Override
        public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
          AccountInsightBatch data = batchData.get(i);
          ps.setInt(1, data.platformAccountId);
          ps.setObject(2, data.totalViews);
          ps.setInt(3, data.totalFollowers);
          ps.setInt(4, data.totalContents);
          ps.setObject(5, data.totalLikes);
          ps.setObject(6, data.totalComments);
          ps.setDouble(7, data.likeScore);
          ps.setObject(8, data.targetDate);
          ps.setObject(9, now);
        }

        @Override
        public int getBatchSize() {
          return batchData.size();
        }
      });

      log.info("✅ Batch Insert 완료: {} 건 저장", batchData.size());

    } catch (Exception e) {
      log.error("❌ Batch Insert 실패: {} 건", batchData.size(), e);
      throw new RuntimeException("DailyAccountInsight Batch 저장 실패", e);
    }
  }

  /**
   * @deprecated 개별 저장 방식 (Batch Insert로 대체됨)
   */
  @Deprecated
  private void saveDailyAccountInsight(String platformType, Row row, LocalDate targetDate, Integer platformAccountId,
      String accountNickname) {
    try {

      if (platformAccountId == null) {
        log.warn("PlatformAccount not found: platformType={}, accountNickname={}",
            platformType, accountNickname);
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
      Double likeScore = calculateLikeScore(totalViews, totalLikes, totalComments, totalFollowers);

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
          platformAccountId, // platform_account_id (INT UNSIGNED)
          totalViews, // total_views (BIGINT UNSIGNED) → BigInteger
          totalFollowers, // total_followers (INT UNSIGNED) → Integer
          totalContents, // total_contents (INT UNSIGNED) → Integer
          totalLikes, // total_likes (BIGINT UNSIGNED) → BigInteger
          totalComments, // total_comments (BIGINT UNSIGNED) → BigInteger
          likeScore, // like_score (DECIMAL(5,2)) → BigInteger (근데 이건 문제...)
          targetDate, // snapshot_date (DATE)
          LocalDateTime.now() // created_at (DATETIME)
      );

    } catch (Exception e) {
      log.error("DailyAccountInsight 저장 실패", e);
    }
  }

  private Double calculateLikeScore(BigInteger totalViews, BigInteger totalLikes, BigInteger totalComments,
      Integer totalFollowers) {
    if (totalViews.equals(BigInteger.ZERO))
      return 0.0;

    double views = totalViews.doubleValue();
    double likes = totalLikes.doubleValue();
    double comments = totalComments.doubleValue();
    double followers = totalFollowers.doubleValue();

    // 참여율 계산 (0~100%)
    double denominator = views != 0.0 ? views : followers;
    double engagementRate = (likes + comments) / denominator * 100;

    // 0~100점 범위로 제한
    double score = Math.min(100.0, Math.max(0.0, engagementRate * 10));

    return Math.round(score * 100.0) / 100.0;
  }

  /**
   * 특정 계정에 대한 DailyAccountInsight 생성 (오버로드)
   * 
   * @param platformType              플랫폼 타입
   * @param targetDate                통계를 생성할 기준 날짜
   * @param specificPlatformAccountId 특정 계정 ID
   */
  public void generateDailyAccountInsight(String platformType, LocalDate targetDate,
      Integer specificPlatformAccountId) {
    try {
      // 특정 계정의 닉네임 조회
      String specificAccountNickname = getAccountNickname(specificPlatformAccountId);
      if (specificAccountNickname == null) {
        log.warn("계정을 찾을 수 없음: platformAccountId={}", specificPlatformAccountId);
        return;
      }

      // 1. 콘텐츠 데이터 읽기 (특정 계정만 필터링)
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate)
          .filter(col("accountNickname").equalTo(specificAccountNickname))
          .cache();

      // 2. 계정 프로필 데이터 읽기 (특정 계정만 필터링)
      Dataset<Row> accountData = readS3AccountData(platformType, targetDate)
          .filter(col("accountNickname").equalTo(specificAccountNickname))
          .cache();

      // 3. 콘텐츠별 통계 집계
      Dataset<Row> contentStatistics = contentData
          .filter(col("accountNickname").isNotNull())
          .groupBy("accountNickname")
          .agg(
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              count("*").alias("totalContents"));

      // 4. 계정 프로필 정보에서 팔로워 수 추출
      Dataset<Row> accountFollowers = accountData
          .filter(col("accountNickname").isNotNull())
          .select("accountNickname", "followersCount")
          .withColumnRenamed("followersCount", "totalFollowers");

      // 5. 콘텐츠 통계 + 계정 정보 조인
      Dataset<Row> joinedData = contentStatistics
          .join(accountFollowers, "accountNickname")
          .select("accountNickname", "totalViews", "totalLikes",
              "totalComments", "totalContents", "totalFollowers");

      // 6. 결과 저장 (1개 계정만)
      List<Row> results = joinedData.collectAsList();

      if (results.isEmpty()) {
        log.warn("해당 계정의 데이터를 찾을 수 없음: {}", specificAccountNickname);
        return;
      }

      Row row = results.get(0);

      // 1) MySQL에 저장
      saveDailyAccountInsight(platformType, row, targetDate, specificPlatformAccountId, specificAccountNickname);

      // 2) S3에도 저장
      saveStatisticsToS3(platformType, row, targetDate, specificPlatformAccountId, specificAccountNickname);

      log.info("특정 계정 인사이트 생성 완료: accountId={}, nickname={}", specificPlatformAccountId, specificAccountNickname);

    } catch (Exception e) {
      log.error("특정 계정 DailyAccountInsight 생성 실패: accountId={}", specificPlatformAccountId, e);
      throw new RuntimeException("특정 계정 DailyAccountInsight 생성 실패", e);
    }
  }

}