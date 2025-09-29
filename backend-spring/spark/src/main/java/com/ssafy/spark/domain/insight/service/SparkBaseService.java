package com.ssafy.spark.domain.insight.service;

import static org.apache.spark.sql.functions.lit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@NoArgsConstructor
public class SparkBaseService {

  @Autowired
  protected SparkSession sparkSession;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected AmazonS3Client amazonS3Client;

  @Value("${cloud.aws.s3.bucket}")
  protected String bucketName;

  /**
   * 특정 날짜의 콘텐츠 데이터 읽기 : Parquet 기반
   */
  public Dataset<Row> readS3ContentDataByDate(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/parquet/%s/content/%s/", bucketName, platformType, dateFolder);

      log.info("콘텐츠 Parquet 데이터 읽기: {}", s3Path);

      return sparkSession.read().parquet(s3Path);

    } catch (Exception e) {
      log.warn("S3 특정날짜 콘텐츠 데이터 읽기 실패: platform={}, date={}",
          platformType, targetDate, e);
      return sparkSession.emptyDataFrame()
          .withColumn("externalContentId", lit("").cast("string"))
          .withColumn("accountNickname", lit("").cast("string"))
          .withColumn("publishedAt", lit("").cast("string"))
          .withColumn("viewsCount", lit(0L).cast("long"))
          .withColumn("likesCount", lit(0L).cast("long"))
          .withColumn("commentsCount", lit(0L).cast("long"));
    }
  }

  /**
   * 특정 날짜의 전체 계정 데이터 읽기 : Parquet 기반
   */
  public Dataset<Row> readS3AccountData(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/parquet/%s/platform_account/%s/", bucketName, platformType,
          dateFolder);

      log.info("계정 Parquet 데이터 읽기: {}", s3Path);

      return sparkSession.read().parquet(s3Path);

    } catch (Exception e) {
      log.error("S3 특정날짜 전체 계정 데이터 읽기 실패: platform={}, date={}", platformType, targetDate, e);
      return sparkSession.emptyDataFrame()
          .withColumn("accountNickname", lit("").cast("string"))
          .withColumn("categoryName", lit("").cast("string"))
          .withColumn("followersCount", lit(0L).cast("long"));
    }
  }

  /**
   * ✅ JSON → Parquet 변환 후 저장 (콘텐츠)
   */
  public void convertContentJsonToParquet(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String jsonPath = String.format("s3a://%s/raw_data/json/%s/content/%s/*.json", bucketName, platformType,
          dateFolder);
      String parquetPath = String.format("s3a://%s/raw_data/parquet/%s/content/%s/", bucketName, platformType,
          dateFolder);

      log.info("[콘텐츠] JSON → Parquet 변환 시작: {}", jsonPath);

      // JSON 읽기 (multiline 모드)
      Dataset<Row> jsonDf = sparkSession.read()
          .option("multiline", "true") // ⭐ 중요: 여러 줄 JSON 처리
          .json(jsonPath);

      long jsonCount = jsonDf.count();
      log.info("[콘텐츠] JSON 데이터 개수: {} rows", jsonCount);

      // 스키마 확인
      log.info("[콘텐츠] JSON 스키마:");
      jsonDf.printSchema();

      // 샘플 데이터는 스키마가 정상일 때만 출력
      if (jsonDf.columns().length > 1) {
        log.info("[콘텐츠] JSON 샘플 데이터:");
        jsonDf.show(3, false);
      } else {
        log.error("[콘텐츠] ❌ JSON 파싱 실패! 모든 파일이 손상되었습니다.");
        throw new RuntimeException("JSON 파일이 모두 손상되었습니다. 파일 형식을 확인하세요.");
      }

      // Parquet 저장
      jsonDf.write()
          .mode("overwrite")
          .option("compression", "snappy")
          .parquet(parquetPath);

      // 저장된 데이터 확인
      Dataset<Row> savedDf = sparkSession.read().parquet(parquetPath);
      long savedCount = savedDf.count();

      log.info("[콘텐츠] Parquet 변환 완료: {} (JSON: {} rows → Parquet: {} rows)",
          parquetPath, jsonCount, savedCount);

      if (jsonCount != savedCount) {
        log.warn("[콘텐츠] ⚠️ 데이터 손실 발생! JSON: {}, Parquet: {}", jsonCount, savedCount);
      } else {
        log.info("[콘텐츠] ✅ 데이터 변환 성공! 모든 레코드가 정상적으로 저장되었습니다.");
      }

    } catch (Exception e) {
      log.error("[콘텐츠] Parquet 변환 실패: platform={}, date={}", platformType, targetDate, e);
      throw new RuntimeException("Parquet 변환 실패", e);
    }
  }

  /**
   * ✅ Parquet 데이터 읽기
   * dataType : content, platform_account
   */
  public Dataset<Row> readS3ParquetData(String platformType, LocalDate targetDate, String dataType) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/parquet/%s/%s/%s/", bucketName, platformType, dataType,
          dateFolder);

      log.info("Parquet 데이터 읽기 시작: {}", s3Path);

      return sparkSession.read().parquet(s3Path);

    } catch (Exception e) {
      log.error("Parquet 데이터 읽기 실패: platform={}, type={}, date={}", platformType, dataType, targetDate, e);
      return sparkSession.emptyDataFrame();
    }
  }

  /**
   * 특정 날짜의 계정 데이터(JSON) → Parquet 변환
   */
  public void convertPlatformAccountJsonToParquet(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String jsonPath = String.format("s3a://%s/raw_data/json/%s/platform_account/%s/*.json", bucketName, platformType,
          dateFolder);
      String parquetPath = String.format("s3a://%s/raw_data/parquet/%s/platform_account/%s/", bucketName, platformType,
          dateFolder);

      log.info("[계정] JSON → Parquet 변환 시작: {}", jsonPath);

      // JSON 읽기 (multiline 모드)
      Dataset<Row> jsonDf = sparkSession.read()
          .option("multiline", "true") // ⭐ 중요: 여러 줄 JSON 처리
          .json(jsonPath);

      long jsonCount = jsonDf.count();
      log.info("[계정] JSON 데이터 개수: {} rows", jsonCount);

      // 스키마 확인
      log.info("[계정] JSON 스키마:");
      jsonDf.printSchema();

      // 샘플 데이터는 스키마가 정상일 때만 출력
      if (jsonDf.columns().length > 1) {
        log.info("[계정] JSON 샘플 데이터:");
        jsonDf.show(3, false);
      } else {
        log.error("[계정] ❌ JSON 파싱 실패! 모든 파일이 손상되었습니다.");
        throw new RuntimeException("JSON 파일이 모두 손상되었습니다. 파일 형식을 확인하세요.");
      }

      // Parquet 저장
      jsonDf.write()
          .mode("overwrite")
          .option("compression", "snappy")
          .parquet(parquetPath);

      // 저장된 데이터 확인
      Dataset<Row> savedDf = sparkSession.read().parquet(parquetPath);
      long savedCount = savedDf.count();

      log.info("[계정] Parquet 변환 완료: {} (JSON: {} rows → Parquet: {} rows)",
          parquetPath, jsonCount, savedCount);

      if (jsonCount != savedCount) {
        log.warn("[계정] ⚠️ 데이터 손실 발생! JSON: {}, Parquet: {}", jsonCount, savedCount);
      } else {
        log.info("[계정] ✅ 데이터 변환 성공! 모든 레코드가 정상적으로 저장되었습니다.");
      }

    } catch (Exception e) {
      log.error("[계정] Parquet 변환 실패: platform={}, date={}", platformType, targetDate, e);
      throw new RuntimeException("Parquet 변환 실패", e);
    }
  }

  /**
   * ✅ 특정 날짜의 계정 Parquet 데이터 읽기
   */
  public Dataset<Row> readAccountParquetData(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data_/parquet/%s/platform_account/%s/", bucketName, platformType,
          dateFolder);

      log.info("[계정] Parquet 데이터 읽기 시작: {}", s3Path);

      return sparkSession.read().parquet(s3Path);
    } catch (Exception e) {
      log.error("[계정] Parquet 데이터 읽기 실패: platform={}, date={}", platformType, targetDate, e);
      return sparkSession.emptyDataFrame();
    }
  }

  /**
   * S3 파일 업로드
   */
  protected String uploadFile(String accessKey, byte[] content, String contentType) {
    try {
      log.info("S3 업로드 시작: s3://{}/{}", bucketName, accessKey);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(content.length);
      metadata.setContentType(contentType);

      PutObjectRequest request = new PutObjectRequest(
          bucketName,
          accessKey,
          new ByteArrayInputStream(content),
          metadata);

      amazonS3Client.putObject(request);
      log.info("S3 업로드 성공: s3://{}/{}", bucketName, accessKey);

      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      log.error("S3 파일 업로드 실패: s3://{}/{}", bucketName, accessKey, e);
      throw new RuntimeException("S3 파일 업로드 실패: " + accessKey, e);
    }
  }

  /**
   * DB 관련 헬퍼 메서드들
   */
  protected BigInteger getBigIntegerValue(Row row, String columnName) {
    Object value = row.getAs(columnName);
    if (value == null)
      return BigInteger.ZERO;
    return new BigInteger(value.toString());
  }

  protected Integer getIntegerValue(Row row, String columnName) {
    Object value = row.getAs(columnName);
    if (value == null)
      return 0;
    return Integer.valueOf(value.toString());
  }

  protected Integer getContentId(String platformType, String externalContentId) {
    try {
      String sql = "SELECT content_id FROM content WHERE platform_type_id = ? AND external_content_id = ? LIMIT 1";
      List<Integer> results = jdbcTemplate.query(sql,
          (rs, rowNum) -> rs.getInt("content_id"),
          platformType, externalContentId);
      return results.isEmpty() ? null : results.get(0);
    } catch (Exception e) {
      log.error("getContentId 매핑 실패: platform={}, externalContentId={}", platformType, externalContentId, e);
      return null;
    }
  }

  protected Integer getCategoryTypeId(String categoryName) {
    try {
      String sql = "SELECT category_type_id FROM category_type WHERE category_name = ? LIMIT 1";
      List<Integer> results = jdbcTemplate.query(sql,
          (rs, rowNum) -> rs.getInt("category_type_id"),
          categoryName);
      return results.isEmpty() ? null : results.get(0);
    } catch (Exception e) {
      log.error("CategoryName 매핑 실패: categoryName={}", categoryName, e);
      return null;
    }
  }

  protected Integer getPlatformAccountId(String platformType, String accountNickname) {
    try {
      String sql = "SELECT platform_account_id FROM platform_account WHERE platform_type_id= ? AND account_nickname = ?";
      return jdbcTemplate.queryForObject(sql, Integer.class, platformType.toUpperCase(), accountNickname);
    } catch (Exception e) {
      log.warn("PlatformAccount 조회 실패: platform={}, accountNickname={}", platformType, accountNickname);
      return null;
    }
  }

  protected Integer getInfluencerIdByPlatformAccount(Integer platformAccountId) {
    if (platformAccountId == null)
      return null;

    String sql = "SELECT influencer_id FROM platform_account WHERE platform_account_id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, Integer.class, platformAccountId);
    } catch (Exception e) {
      log.warn("influencer_id 조회 실패: platformAccountId={}", platformAccountId);
      return null;
    }
  }

  protected String getAccountNickname(Integer platformAccountId) {
    try {
      String sql = "SELECT account_nickname FROM platform_account WHERE platform_account_id = ?";
      return jdbcTemplate.queryForObject(sql, String.class, platformAccountId);
    } catch (Exception e) {
      log.error("계정 닉네임 조회 실패: platformAccountId={}", platformAccountId, e);
      return null;
    }
  }
}
