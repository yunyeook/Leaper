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
import java.util.Map;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkTypeInsightService extends SparkBaseService {

  /**
   * DailyTypeInsight 생성 - 계정별 + 콘텐츠 타입별 통계
   */
  public void generateDailyTypeInsight(String platformType, LocalDate targetDate) {
    try {
      // 1. 콘텐츠 데이터 읽기
      Dataset<Row> contentData = readS3ContentDataByDate(platformType, targetDate);

      Dataset<Row> typeStatistics = contentData
          .filter(col("accountNickname").isNotNull())
          .filter(col("contentType").isNotNull())
          .groupBy("accountNickname", "contentType")
          .agg(
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              count("*").alias("totalContents")
          );

      // 2. Spark → Java List 변환
      List<Row> results = typeStatistics.collectAsList();
      log.info("콘텐츠 타입별 통계 개수: {}", results.size());

      // 3. DB에서 "어제", "전달 말일" 스냅샷 불러오기
      LocalDate yesterday = targetDate.minusDays(1);
      LocalDate lastMonthEnd = targetDate.withDayOfMonth(1).minusDays(1);

      Map<String, SnapshotRow> yesterdaySnapshot = loadSnapshotFromDB(yesterday);
      Map<String, SnapshotRow> lastMonthSnapshot = loadSnapshotFromDB(lastMonthEnd);

      // 4. 결과 저장
      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);
        String contentType = row.getAs("contentType");


        saveDailyTypeInsight( row, targetDate, yesterdaySnapshot, lastMonthSnapshot,platformAccountId, accountNickname,contentType);
        saveTypeStatisticsToS3(platformType, row, targetDate, yesterdaySnapshot, lastMonthSnapshot,platformAccountId, accountNickname,contentType);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyTypeInsight 생성 실패", e);
    }
  }

  /**
   * DB 저장
   */
  private void saveDailyTypeInsight( Row row, LocalDate targetDate,
      Map<String, SnapshotRow> yesterdaySnapshot, Map<String, SnapshotRow> lastMonthSnapshot,
      Integer platformAccountId, String accountNickname, String contentType
  ) {
    try {

      // 오늘 total 값
      BigInteger totalViews = getBigIntegerValue(row, "totalViews");
      BigInteger totalLikes = getBigIntegerValue(row, "totalLikes");
      Integer totalContents = getIntegerValue(row, "totalContents");

      // 증가량 계산
      String key = accountNickname + "_" + contentType;
      SnapshotRow yesterday = yesterdaySnapshot.getOrDefault(key, new SnapshotRow());
      SnapshotRow lastMonth = lastMonthSnapshot.getOrDefault(key, new SnapshotRow());

      int todayViews = Math.max(0, totalViews.subtract(yesterday.getViews()).intValue());
      int todayLikes = Math.max(0, totalLikes.subtract(yesterday.getLikes()).intValue());
      int todayContents = Math.max(0, totalContents - yesterday.getContents());

      BigInteger monthViews = totalViews.subtract(lastMonth.getViews()).max(BigInteger.ZERO);
      BigInteger monthLikes = totalLikes.subtract(lastMonth.getLikes()).max(BigInteger.ZERO);
      int monthContents = Math.max(0, totalContents - lastMonth.getContents());

      // DB insert/update
      String sql = "INSERT INTO daily_type_insight " +
          "(content_type_id, platform_account_id, today_views, today_contents, today_likes, " +
          " month_views, month_contents, month_likes, total_views, total_contents, total_likes, " +
          " snapshot_date, created_at) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "today_views = VALUES(today_views), " +
          "today_contents = VALUES(today_contents), " +
          "today_likes = VALUES(today_likes), " +
          "month_views = VALUES(month_views), " +
          "month_contents = VALUES(month_contents), " +
          "month_likes = VALUES(month_likes), " +
          "total_views = VALUES(total_views), " +
          "total_contents = VALUES(total_contents), " +
          "total_likes = VALUES(total_likes), " +
          "snapshot_date = VALUES(snapshot_date), " +
          "created_at = VALUES(created_at)";

      jdbcTemplate.update(sql,
          contentType,
          platformAccountId,
          todayViews,
          todayContents,
          todayLikes,
          monthViews,
          monthContents,
          monthLikes,
          totalViews,
          totalContents,
          totalLikes,
          targetDate,
          LocalDateTime.now()
      );

    } catch (Exception e) {
      log.error("DailyTypeInsight 저장 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * S3 저장
   */
  private void saveTypeStatisticsToS3(String platformType, Row row, LocalDate targetDate,
      Map<String, SnapshotRow> yesterdaySnapshot, Map<String, SnapshotRow> lastMonthSnapshot,
      Integer platformAccountId, String accountNickname, String contentType
  ) {
    try {

      BigInteger totalViews = getBigIntegerValue(row, "totalViews");
      BigInteger totalLikes = getBigIntegerValue(row, "totalLikes");
      Integer totalContents = getIntegerValue(row, "totalContents");

      // 증가량 계산
      String key = accountNickname + "_" + contentType;
      SnapshotRow yesterday = yesterdaySnapshot.getOrDefault(key, new SnapshotRow());
      SnapshotRow lastMonth = lastMonthSnapshot.getOrDefault(key, new SnapshotRow());

      int todayViews = totalViews.subtract(yesterday.getViews()).intValue();
      int todayLikes = totalLikes.subtract(yesterday.getLikes()).intValue();
      int todayContents = totalContents - yesterday.getContents();

      BigInteger monthViews = totalViews.subtract(lastMonth.getViews());
      BigInteger monthLikes = totalLikes.subtract(lastMonth.getLikes());
      int monthContents = totalContents - lastMonth.getContents();

      // JSON 변환
      ObjectNode statisticsJson = objectMapper.createObjectNode();
      statisticsJson.put("platformAccountId", platformAccountId);
      statisticsJson.put("platformType", platformType.toUpperCase());
      statisticsJson.put("contentType", contentType);

      statisticsJson.put("todayViews", todayViews);
      statisticsJson.put("todayLikes", todayLikes);
      statisticsJson.put("todayContents", todayContents);

      statisticsJson.put("monthViews", monthViews.toString());
      statisticsJson.put("monthLikes", monthLikes.toString());
      statisticsJson.put("monthContents", monthContents);

      statisticsJson.put("totalViews", totalViews.toString());
      statisticsJson.put("totalLikes", totalLikes.toString());
      statisticsJson.put("totalContents", totalContents);

      statisticsJson.put("snapshotDate", targetDate.toString());
      statisticsJson.put("processedAt", LocalDateTime.now().toString());

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statisticsJson);

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_type_insight_%s_%s_%s.json",
          accountNickname,
          contentType.toLowerCase(),
          timestamp);
      String s3Path = String.format("processed_data/%s/daily_type_insight/%s/%s/%s/%s",
          platformType,
          targetDate.getYear(),
          String.format("%02d", targetDate.getMonthValue()),
          String.format("%02d", targetDate.getDayOfMonth()),
          fileName);

      uploadFile(s3Path, jsonData.getBytes(), "application/json");

    } catch (Exception e) {
      log.error("S3 타입별 통계 저장 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * DB에서 특정 일자의 스냅샷 불러오기
   */
  /**
   * DB에서 특정 일자의 스냅샷 불러오기
   */
  private Map<String, SnapshotRow> loadSnapshotFromDB( LocalDate date) {
    String sql = "SELECT da.content_type_id, da.platform_account_id, da.total_views, da.total_likes, da.total_contents, pa.external_account_id " +
        "FROM daily_type_insight da " +
        "JOIN platform_account pa ON da.platform_account_id = pa.platform_account_id " +
        "WHERE da.snapshot_date = ?";

    return jdbcTemplate.query(sql, new Object[]{date}, rs -> {
      Map<String, SnapshotRow> map = new java.util.HashMap<>();
      while (rs.next()) {
        String externalAccountId = rs.getString("external_account_id");
        String contentType = rs.getString("content_type_id");
        String key = externalAccountId + "_" + contentType;
        map.put(key, new SnapshotRow(
            rs.getBigDecimal("total_views").toBigInteger(),
            rs.getBigDecimal("total_likes").toBigInteger(),
            rs.getInt("total_contents")
        ));
      }
      return map;
    });
  }

  /**
   * 특정 계정에 대한 DailyTypeInsight 생성 (오버로드)
   */
  public void generateDailyTypeInsight(String platformType, LocalDate targetDate, Integer specificPlatformAccountId) {
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

      // 2. 콘텐츠 타입별 통계 집계
      Dataset<Row> typeStatistics = contentData
          .filter(col("accountNickname").isNotNull())
          .filter(col("contentType").isNotNull())
          .groupBy("accountNickname", "contentType")
          .agg(
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              count("*").alias("totalContents")
          );

      // 3. 결과 수집
      List<Row> results = typeStatistics.collectAsList();
      if (results.isEmpty()) {
        log.warn("해당 계정의 데이터를 찾을 수 없음: {}", specificAccountNickname);
        return;
      }

      // 4. DB에서 "어제", "전달 말일" 스냅샷 불러오기
      LocalDate yesterday = targetDate.minusDays(1);
      LocalDate lastMonthEnd = targetDate.withDayOfMonth(1).minusDays(1);

      Map<String, SnapshotRow> yesterdaySnapshot = loadSnapshotFromDB(yesterday);
      Map<String, SnapshotRow> lastMonthSnapshot = loadSnapshotFromDB(lastMonthEnd);

      // 5. 결과 저장
      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        String contentType = row.getAs("contentType");

        saveDailyTypeInsight(row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            specificPlatformAccountId, accountNickname, contentType);

        saveTypeStatisticsToS3(platformType, row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            specificPlatformAccountId, accountNickname, contentType);
      }

      log.info("특정 계정 DailyTypeInsight 생성 완료: accountId={}, nickname={}",
          specificPlatformAccountId, specificAccountNickname);

    } catch (Exception e) {
      log.error("특정 계정 DailyTypeInsight 생성 실패: accountId={}", specificPlatformAccountId, e);
      throw new RuntimeException("특정 계정 DailyTypeInsight 생성 실패", e);
    }
  }


  /**
   * 스냅샷 값 담는 DTO
   */
  private static class SnapshotRow {
    private final BigInteger views;
    private final BigInteger likes;
    private final int contents;

    public SnapshotRow() {
      this.views = BigInteger.ZERO;
      this.likes = BigInteger.ZERO;
      this.contents = 0;
    }

    public SnapshotRow(BigInteger views, BigInteger likes, int contents) {
      this.views = views;
      this.likes = likes;
      this.contents = contents;
    }

    public BigInteger getViews() { return views; }
    public BigInteger getLikes() { return likes; }
    public int getContents() { return contents; }
  }
}
