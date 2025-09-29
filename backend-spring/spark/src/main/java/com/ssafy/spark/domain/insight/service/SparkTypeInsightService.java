package com.ssafy.spark.domain.insight.service;

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
  public void generateDailyTypeInsight(String platformType, LocalDate targetDate, Dataset<Row> contentData) {
    try {

      Dataset<Row> typeStatistics = contentData
          .filter(col("accountNickname").isNotNull())
          .filter(col("contentType").isNotNull())
          .groupBy("accountNickname", "contentType")
          .agg(
              sum(when(col("viewsCount").isNotNull(), col("viewsCount")).otherwise(0)).alias("totalViews"),
              sum(when(col("likesCount").isNotNull(), col("likesCount")).otherwise(0)).alias("totalLikes"),
              sum(when(col("commentsCount").isNotNull(), col("commentsCount")).otherwise(0)).alias("totalComments"),
              count("*").alias("totalContents"));

      // 2. Spark → Java List 변환
      List<Row> results = typeStatistics.collectAsList();
      log.info("콘텐츠 타입별 통계 개수: {}", results.size());

      // 3. DB에서 "어제", "전달 말일" 스냅샷 불러오기
      LocalDate yesterday = targetDate.minusDays(1);
      LocalDate lastMonthEnd = targetDate.withDayOfMonth(1).minusDays(1);

      Map<String, SnapshotRow> yesterdaySnapshot = loadSnapshotFromDB(yesterday);
      Map<String, SnapshotRow> lastMonthSnapshot = loadSnapshotFromDB(lastMonthEnd);

      // 4. 결과 저장 준비 (Batch 처리를 위한 리스트)
      List<DailyTypeInsightDto> batchList = new java.util.ArrayList<>();

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        Integer platformAccountId = getPlatformAccountId(platformType.toUpperCase(), accountNickname);
        String contentType = row.getAs("contentType");

        // DB에 저장할 데이터 계산 및 DTO 생성
        DailyTypeInsightDto dto = calculateDailyTypeInsight(row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            platformAccountId, accountNickname, contentType);
        batchList.add(dto);

        // S3 저장은 기존대로 유지 (별도 파일 생성)
        saveTypeStatisticsToS3(platformType, row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            platformAccountId, accountNickname, contentType);

        // 1000개씩 끊어서 저장 (메모리 절약)
        if (batchList.size() >= 1000) {
          saveDailyTypeInsightBatch(batchList);
          batchList.clear();
        }
      }

      // 남은 데이터 저장
      if (!batchList.isEmpty()) {
        saveDailyTypeInsightBatch(batchList);
      }

    } catch (Exception e) {
      throw new RuntimeException("DailyTypeInsight 생성 실패", e);
    }
  }

  /**
   * DB 저장용 DTO 계산
   */
  private DailyTypeInsightDto calculateDailyTypeInsight(Row row, LocalDate targetDate,
      Map<String, SnapshotRow> yesterdaySnapshot, Map<String, SnapshotRow> lastMonthSnapshot,
      Integer platformAccountId, String accountNickname, String contentType) {

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

    return DailyTypeInsightDto.builder()
        .contentTypeId(contentType)
        .platformAccountId(platformAccountId)
        .todayViews(todayViews)
        .todayContents(todayContents)
        .todayLikes(todayLikes)
        .monthViews(monthViews)
        .monthContents(monthContents)
        .monthLikes(monthLikes)
        .totalViews(totalViews)
        .totalContents(totalContents)
        .totalLikes(totalLikes)
        .snapshotDate(java.sql.Date.valueOf(targetDate))
        .createdAt(java.sql.Timestamp.valueOf(LocalDateTime.now()))
        .build();
  }

  /**
   * JDBC Batch Update 실행
   */
  private void saveDailyTypeInsightBatch(List<DailyTypeInsightDto> dtoList) {
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

    jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
      @Override
      public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
        DailyTypeInsightDto dto = dtoList.get(i);
        ps.setString(1, dto.getContentTypeId());
        ps.setInt(2, dto.getPlatformAccountId());
        ps.setInt(3, dto.getTodayViews());
        ps.setInt(4, dto.getTodayContents());
        ps.setInt(5, dto.getTodayLikes());
        ps.setObject(6, dto.getMonthViews()); // BigInteger
        ps.setInt(7, dto.getMonthContents());
        ps.setObject(8, dto.getMonthLikes()); // BigInteger
        ps.setObject(9, dto.getTotalViews()); // BigInteger
        ps.setInt(10, dto.getTotalContents());
        ps.setObject(11, dto.getTotalLikes()); // BigInteger
        ps.setDate(12, dto.getSnapshotDate());
        ps.setTimestamp(13, dto.getCreatedAt());
      }

      @Override
      public int getBatchSize() {
        return dtoList.size();
      }
    });

    log.info("DailyTypeInsight Batch 저장 완료: {} 건", dtoList.size());
  }

  @lombok.Builder
  @lombok.Getter
  private static class DailyTypeInsightDto {
    private String contentTypeId;
    private Integer platformAccountId;
    private int todayViews;
    private int todayContents;
    private int todayLikes;
    private BigInteger monthViews;
    private int monthContents;
    private BigInteger monthLikes;
    private BigInteger totalViews;
    private int totalContents;
    private BigInteger totalLikes;
    private java.sql.Date snapshotDate;
    private java.sql.Timestamp createdAt;
  }

  /**
   * S3 저장
   */
  private void saveTypeStatisticsToS3(String platformType, Row row, LocalDate targetDate,
      Map<String, SnapshotRow> yesterdaySnapshot, Map<String, SnapshotRow> lastMonthSnapshot,
      Integer platformAccountId, String accountNickname, String contentType) {
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

      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

      // ✅ 통일된 경로 구조
      String s3Path = String.format("processed_data/json/%s/daily_type_insight/%s/%s",
          platformType, dateFolder, fileName);

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
  private Map<String, SnapshotRow> loadSnapshotFromDB(LocalDate date) {
    String sql = "SELECT da.content_type_id, da.platform_account_id, da.total_views, da.total_likes, da.total_contents, pa.external_account_id "
        +
        "FROM daily_type_insight da " +
        "JOIN platform_account pa ON da.platform_account_id = pa.platform_account_id " +
        "WHERE da.snapshot_date = ?";

    return jdbcTemplate.query(sql, new Object[] { date }, rs -> {
      Map<String, SnapshotRow> map = new java.util.HashMap<>();
      while (rs.next()) {
        String externalAccountId = rs.getString("external_account_id");
        String contentType = rs.getString("content_type_id");
        String key = externalAccountId + "_" + contentType;
        map.put(key, new SnapshotRow(
            rs.getBigDecimal("total_views").toBigInteger(),
            rs.getBigDecimal("total_likes").toBigInteger(),
            rs.getInt("total_contents")));
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
              count("*").alias("totalContents"));

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
      // 5. 결과 저장 및 Batch 처리 준비
      List<DailyTypeInsightDto> batchList = new java.util.ArrayList<>();

      for (Row row : results) {
        String accountNickname = row.getAs("accountNickname");
        String contentType = row.getAs("contentType");

        // DB 저장용 DTO 생성 및 리스트 추가
        DailyTypeInsightDto dto = calculateDailyTypeInsight(row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            specificPlatformAccountId, accountNickname, contentType);
        batchList.add(dto);

        // S3 저장 (기존 유지)
        saveTypeStatisticsToS3(platformType, row, targetDate, yesterdaySnapshot, lastMonthSnapshot,
            specificPlatformAccountId, accountNickname, contentType);
      }

      // Batch Insert 실행
      if (!batchList.isEmpty()) {
        saveDailyTypeInsightBatch(batchList);
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

    public BigInteger getViews() {
      return views;
    }

    public BigInteger getLikes() {
      return likes;
    }

    public int getContents() {
      return contents;
    }
  }
}
