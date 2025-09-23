package com.ssafy.spark.domain.spark.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkDummyDataGeneratorService extends SparkBaseService {

  private final Random random = new Random();

  /**
   * daily_account_insight 더미데이터 365일치 생성
   * @param platformType 플랫폼 타입 (예: "instagram")
   * @param baseDate 기준 날짜 (예: LocalDate.of(2025, 9, 22))
   */
  public void generateDummyAccountInsight(String platformType, LocalDate baseDate) {
    try {
      log.info("Account insight 더미데이터 생성 시작 - Platform: {}, BaseDate: {}", platformType, baseDate);

      // 1. 기존 S3 데이터 읽기
      List<JsonNode> baseDataList = readAccountInsightFromS3(platformType, baseDate);
      log.info("기존 데이터 {}개 발견", baseDataList.size());

      for (JsonNode baseData : baseDataList) {
        Long platformAccountId = baseData.get("platformAccountId").asLong();
        BigInteger currentTotalViews = new BigInteger(baseData.get("totalViews").asText());
        int currentTotalFollowers = baseData.get("totalFollowers").asInt();
        int currentTotalContents = baseData.get("totalContents").asInt();
        BigInteger currentTotalLikes = new BigInteger(baseData.get("totalLikes").asText());
        BigInteger currentTotalComments = new BigInteger(baseData.get("totalComments").asText());

        log.info("계정 ID {} 처리 시작", platformAccountId);

        // 2. 365일 역순으로 데이터 생성
        for (int i = 1; i <=365; i++) {
          LocalDate targetDate = baseDate.minusDays(i);

          // 3. 랜덤값으로 감소시키기
          // totalViews: 0~10% 감소
          double viewsDecreaseRate = random.nextDouble() * 0.1;
          BigInteger totalViews = currentTotalViews.subtract(
              currentTotalViews.multiply(BigInteger.valueOf((long)(viewsDecreaseRate * 100)))
                  .divide(BigInteger.valueOf(100))
          );

          // totalFollowers, totalContents: 90% 확률로 0 감소, 10% 확률로 1 감소
          int totalFollowers = Math.max(0, currentTotalFollowers - (random.nextDouble() < 0.9 ? 0 : 1));
          int totalContents = Math.max(0, currentTotalContents - (random.nextDouble() < 0.9 ? 0 : 1));

          // totalLikes, totalComments도 비슷하게 감소
          BigInteger totalLikes = currentTotalLikes.subtract(
              currentTotalLikes.multiply(BigInteger.valueOf((long)(viewsDecreaseRate * 100)))
                  .divide(BigInteger.valueOf(100))
          );
          BigInteger totalComments = currentTotalComments.subtract(
              currentTotalComments.multiply(BigInteger.valueOf((long)(viewsDecreaseRate * 100)))
                  .divide(BigInteger.valueOf(100))
          );

          // 4. S3에 저장
          saveAccountInsightToS3(platformType, targetDate, platformAccountId,
              totalViews, totalFollowers, totalContents, totalLikes, totalComments);

          // 5. DB에 저장
          saveAccountInsightToDB(platformAccountId, totalViews, totalFollowers,
              totalContents, totalLikes, totalComments, targetDate);

          // 6. 다음 반복을 위해 current값 업데이트
          currentTotalViews = totalViews;
          currentTotalFollowers = totalFollowers;
          currentTotalContents = totalContents;
          currentTotalLikes = totalLikes;
          currentTotalComments = totalComments;
        }

        log.info("계정 ID {} 처리 완료", platformAccountId);
      }

      log.info("Account insight 더미데이터 생성 완료");
    } catch (Exception e) {
      log.error("Account insight 더미데이터 생성 실패", e);
      throw new RuntimeException("Account insight 더미데이터 생성 실패", e);
    }
  }

  /**
   * daily_type_insight 더미데이터 365일치 생성
   * @param platformType 플랫폼 타입 (예: "instagram")
   * @param baseDate 기준 날짜 (예: LocalDate.of(2025, 9, 22))
   */
  public void generateDummyTypeInsight(String platformType, LocalDate baseDate) {
    try {
      log.info("Type insight 더미데이터 생성 시작 - Platform: {}, BaseDate: {}", platformType, baseDate);

      // 1. 기존 S3 데이터 읽기
      List<JsonNode> baseDataList = readTypeInsightFromS3(platformType, baseDate);
      log.info("기존 데이터 {}개 발견", baseDataList.size());

      for (JsonNode baseData : baseDataList) {
        Long platformAccountId = baseData.get("platformAccountId").asLong();
        String contentType = baseData.get("contentType").asText();
        BigInteger currentTotalViews = new BigInteger(baseData.get("totalViews").asText());
        int currentTotalContents = baseData.get("totalContents").asInt();
        BigInteger currentTotalLikes = new BigInteger(baseData.get("totalLikes").asText());

        log.info("계정 ID {}, 컨텐츠 타입 {} 처리 시작", platformAccountId, contentType);

        // 2. 365일 역순으로 데이터 생성
        for (int i = 1; i <= 365; i++) {
          LocalDate targetDate = baseDate.minusDays(i);

          // 3. today 값들을 현재 total의 합리적 범위 내에서 생성
          int todayViews = Math.min(30, Math.max(1, currentTotalViews.intValue() / 1000));
          int todayContents = Math.min(30, Math.max(0, currentTotalContents / 100));
          int todayLikes = Math.min(30, Math.max(1, currentTotalLikes.intValue() / 1000));

          // 4. total 값들 계산 (이전 total에서 today값 빼기)
          BigInteger totalViews = currentTotalViews.subtract(BigInteger.valueOf(todayViews));
          if (totalViews.compareTo(BigInteger.ZERO) < 0) {
            totalViews = BigInteger.ZERO;
          }

          int totalContents = Math.max(0, currentTotalContents - todayContents);

          BigInteger totalLikes = currentTotalLikes.subtract(BigInteger.valueOf(todayLikes));
          if (totalLikes.compareTo(BigInteger.ZERO) < 0) {
            totalLikes = BigInteger.ZERO;
          }

          // 5. month 값들을 today의 20~30배로 합리적 계산
          long monthViews = todayViews * (20L + random.nextInt(11));
          int monthContents = todayContents * (20 + random.nextInt(11));
          long monthLikes = todayLikes * (20L + random.nextInt(11));

          // 6. S3에 저장
          saveTypeInsightToS3(platformType, targetDate, platformAccountId, contentType,
              todayViews, todayContents, todayLikes, monthViews, monthContents, monthLikes,
              totalViews, totalContents, totalLikes);

          // 7. DB에 저장
          saveTypeInsightToDB(platformAccountId, contentType, todayViews, todayContents, todayLikes,
              monthViews, monthContents, monthLikes, totalViews, totalContents, totalLikes, targetDate);

          // 8. 다음 반복을 위해 current값 업데이트
          currentTotalViews = totalViews;
          currentTotalContents = totalContents;
          currentTotalLikes = totalLikes;
        }

        log.info("계정 ID {}, 컨텐츠 타입 {} 처리 완료", platformAccountId, contentType);
      }

      log.info("Type insight 더미데이터 생성 완료");
    } catch (Exception e) {
      log.error("Type insight 더미데이터 생성 실패", e);
      throw new RuntimeException("Type insight 더미데이터 생성 실패", e);
    }
  }

  /**
   * 두 개의 더미데이터를 한 번에 생성
   */
  public void generateAllDummyData(String platformType, LocalDate baseDate) {
    log.info("전체 더미데이터 생성 시작 - Platform: {}, BaseDate: {}", platformType, baseDate);

    generateDummyAccountInsight(platformType, baseDate);
    generateDummyTypeInsight(platformType, baseDate);

    log.info("전체 더미데이터 생성 완료");
  }

  /**
   * S3에서 기존 daily_account_insight 데이터 읽기
   */
  private List<JsonNode> readAccountInsightFromS3(String platformType, LocalDate date) {
    try {
      String s3Prefix = String.format("processed_data/%s/daily_account_insight/%s/%02d/%02d/",
          platformType, date.getYear(), date.getMonthValue(), date.getDayOfMonth());

      return readS3JsonFiles(s3Prefix);
    } catch (Exception e) {
      log.error("S3에서 account insight 데이터 읽기 실패: {}", date, e);
      throw new RuntimeException("S3 데이터 읽기 실패", e);
    }
  }

  /**
   * S3에서 기존 daily_type_insight 데이터 읽기
   */
  private List<JsonNode> readTypeInsightFromS3(String platformType, LocalDate date) {
    try {
      String s3Prefix = String.format("processed_data/%s/daily_type_insight/%s/%02d/%02d/",
          platformType, date.getYear(), date.getMonthValue(), date.getDayOfMonth());

      return readS3JsonFiles(s3Prefix);
    } catch (Exception e) {
      log.error("S3에서 type insight 데이터 읽기 실패: {}", date, e);
      throw new RuntimeException("S3 데이터 읽기 실패", e);
    }
  }

  /**
   * Account Insight S3에 저장
   */
  private void saveAccountInsightToS3(String platformType, LocalDate date, Long platformAccountId,
      BigInteger totalViews, int totalFollowers, int totalContents, BigInteger totalLikes, BigInteger totalComments) {

    try {
      ObjectNode jsonData = objectMapper.createObjectNode();
      jsonData.put("platformAccountId", platformAccountId);
      jsonData.put("platformType", platformType.toUpperCase());
      jsonData.put("totalViews", totalViews.toString());
      jsonData.put("totalLikes", totalLikes.toString());
      jsonData.put("totalComments", totalComments.toString());
      jsonData.put("totalContents", totalContents);
      jsonData.put("totalFollowers", totalFollowers);
      jsonData.put("snapshotDate", date.toString());
      jsonData.put("processedAt", date.atTime(LocalTime.now()).toString());

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_account_insight_%d_%s.json", platformAccountId, timestamp);
      String s3Path = String.format("processed_data/%s/daily_account_insight/%s/%02d/%02d/%s",
          platformType, date.getYear(), date.getMonthValue(), date.getDayOfMonth(), fileName);

      String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData);
      uploadFile(s3Path, jsonString.getBytes(), "application/json");

    } catch (Exception e) {
      log.error("Account Insight S3 저장 실패 - 계정: {}, 날짜: {}", platformAccountId, date, e);
    }
  }

  /**
   * Type Insight S3에 저장
   */
  private void saveTypeInsightToS3(String platformType, LocalDate date, Long platformAccountId, String contentType,
      int todayViews, int todayContents, int todayLikes, long monthViews, int monthContents, long monthLikes,
      BigInteger totalViews, int totalContents, BigInteger totalLikes) {

    try {
      ObjectNode jsonData = objectMapper.createObjectNode();
      jsonData.put("platformAccountId", platformAccountId);
      jsonData.put("platformType", platformType.toUpperCase());
      jsonData.put("contentType", contentType);
      jsonData.put("todayViews", todayViews);
      jsonData.put("todayLikes", todayLikes);
      jsonData.put("todayContents", todayContents);
      jsonData.put("monthViews", monthViews);
      jsonData.put("monthLikes", monthLikes);
      jsonData.put("monthContents", monthContents);
      jsonData.put("totalViews", totalViews.toString());
      jsonData.put("totalLikes", totalLikes.toString());
      jsonData.put("totalContents", totalContents);
      jsonData.put("snapshotDate", date.toString());
      jsonData.put("processedAt", date.atTime(LocalTime.now()).toString());

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String fileName = String.format("daily_type_insight_%d_%s_%s.json", platformAccountId, contentType, timestamp);
      String s3Path = String.format("processed_data/%s/daily_type_insight/%s/%02d/%02d/%s",
          platformType, date.getYear(), date.getMonthValue(), date.getDayOfMonth(), fileName);

      String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData);
      uploadFile(s3Path, jsonString.getBytes(), "application/json");

    } catch (Exception e) {
      log.error("Type Insight S3 저장 실패 - 계정: {}, 타입: {}, 날짜: {}", platformAccountId, contentType, date, e);
    }
  }

  /**
   * Account Insight DB에 저장
   */
  private void saveAccountInsightToDB(Long platformAccountId, BigInteger totalViews, int totalFollowers,
      int totalContents, BigInteger totalLikes, BigInteger totalComments, LocalDate snapshotDate) {

    try {
      // like_score 계산 (간단한 호감도 점수)
      Double likeScore = calculateLikeScore(totalViews, totalLikes, totalComments);

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

      jdbcTemplate.update(sql,
          platformAccountId,     // platform_account_id
          totalViews,            // total_views
          totalFollowers,        // total_followers
          totalContents,         // total_contents
          totalLikes,            // total_likes
          totalComments,         // total_comments
          likeScore,             // like_score
          snapshotDate,          // snapshot_date
          snapshotDate.atTime(LocalTime.now())   // created_at
      );

    } catch (Exception e) {
      log.error("Account Insight DB 저장 실패 - 계정: {}, 날짜: {}", platformAccountId, snapshotDate, e);
    }
  }

  /**
   * Type Insight DB에 저장
   */
  private void saveTypeInsightToDB(Long platformAccountId, String contentType, int todayViews, int todayContents, int todayLikes,
      long monthViews, int monthContents, long monthLikes, BigInteger totalViews, int totalContents, BigInteger totalLikes, LocalDate snapshotDate) {

    try {
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
          contentType,           // content_type_id
          platformAccountId,     // platform_account_id
          todayViews,            // today_views
          todayContents,         // today_contents
          todayLikes,            // today_likes
          monthViews,            // month_views
          monthContents,         // month_contents
          monthLikes,            // month_likes
          totalViews,            // total_views
          totalContents,         // total_contents
          totalLikes,            // total_likes
          snapshotDate,          // snapshot_date
          snapshotDate.atTime(LocalTime.now())   // created_at
      );

    } catch (Exception e) {
      log.error("Type Insight DB 저장 실패 - 계정: {}, 타입: {}, 날짜: {}", platformAccountId, contentType, snapshotDate, e);
    }
  }

  /**
   * 호감도 점수 계산 (SparkAccountInsightService와 동일한 로직)
   */
  private Double calculateLikeScore(BigInteger totalViews, BigInteger totalLikes, BigInteger totalComments) {
    if (totalViews.equals(BigInteger.ZERO)) return 0.0;

    // BigInteger를 double로 변환해서 계산
    double views = totalViews.doubleValue();
    double likes = totalLikes.doubleValue();
    double comments = totalComments.doubleValue();

    // 간단한 호감도 점수 계산
    double engagementRate = (likes + comments) / views * 100;
    double score = Math.min(100.0, Math.max(-100.0, engagementRate * 10 - 50));

    return Math.round(score * 100.0) / 100.0;
  }
}