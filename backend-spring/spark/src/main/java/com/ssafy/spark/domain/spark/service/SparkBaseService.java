package com.ssafy.spark.domain.spark.service;

import static org.apache.spark.sql.functions.lit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
@NoArgsConstructor  // 기본 생성자만
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
   * S3에서 JSON 파일들 읽기 - 공통 메소드
   */
  protected List<JsonNode> readS3JsonFiles(String prefix) {
    List<JsonNode> jsonList = new ArrayList<>();

    try {
      var objectListing = amazonS3Client.listObjects(bucketName, prefix);
      log.info("S3에서 찾은 파일 개수: {}", objectListing.getObjectSummaries().size());

      for (var summary : objectListing.getObjectSummaries()) {
        if (summary.getKey().endsWith(".json")) {
          try (var s3Object = amazonS3Client.getObject(bucketName, summary.getKey());
              var inputStream = s3Object.getObjectContent()) {

            // 전체 파일 내용을 한 번에 읽기
            String content = new String(inputStream.readAllBytes());

            if (!content.trim().isEmpty()) {
              try {
                JsonNode jsonNode = objectMapper.readTree(content);
                jsonList.add(jsonNode);
                log.debug("파일 파싱 성공: {}", summary.getKey());
              } catch (Exception parseEx) {
                log.warn("JSON 파싱 실패, 파일: {}, 내용 미리보기: {}",
                    summary.getKey(),
                    content.length() > 100 ? content.substring(0, 100) : content);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("S3 JSON 파일 읽기 실패: {}", prefix, e);
    }

    log.info("최종 파싱된 JSON 개수: {}", jsonList.size());
    return jsonList;
  }

  /**
   * 특정 날짜의 콘텐츠 데이터 읽기 : 주로 오늘 크롤링 한 데이터를 읽음.
   */
  protected Dataset<Row> readS3ContentDataByDate(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/%s/content/%s/*.json", bucketName,platformType, dateFolder);

      return sparkSession.read()
          .option("multiline", "true")
          .json(s3Path);

    } catch (Exception e) {
      log.warn("S3 특정날짜 콘텐츠 데이터 읽기 실패: platform={}, date={}",
          platformType, targetDate, e);
      // 빈 DataFrame을 필요한 컬럼과 함께 생성
      return sparkSession.emptyDataFrame()
          .withColumn("externalContentId", lit("").cast("string"))
          .withColumn("accountNickname", lit("").cast("string"))
          .withColumn("viewsCount", lit(0L).cast("long"))
          .withColumn("likesCount", lit(0L).cast("long"))
          .withColumn("commentsCount", lit(0L).cast("long"));
    }
  }

  /**
   * 특정 날짜의 전체 계정 데이터 읽기
   */
  protected Dataset<Row> readS3AccountData(String platformType, LocalDate targetDate) {
    try {
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/%s/platform_account/%s/*.json",bucketName, platformType,dateFolder);

      return sparkSession.read()
          .option("multiline", "true")
          .json(s3Path);

    } catch (Exception e) {
      log.error("S3 특정날짜 전체 계정 데이터 읽기 실패: platform={}, date={}", platformType,targetDate, e);
      // 빈 DataFrame을 필요한 컬럼과 함께 생성
      return sparkSession.emptyDataFrame()
          .withColumn("accountNickname", lit("").cast("string"))
          .withColumn("categoryName", lit("").cast("string"))
          .withColumn("followersCount", lit(0L).cast("long"));
    }
  }


  /**
   * S3 파일 업로드 공통 메소드
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
          metadata
      );

      amazonS3Client.putObject(request);
      log.info("S3 업로드 성공: s3://{}/{}", bucketName, accessKey);

      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      log.error("S3 파일 업로드 실패: s3://{}/{}", bucketName, accessKey, e);

      throw new RuntimeException("S3 파일 업로드 실패: " + accessKey, e);
    }
  }

  /**
   *  헬퍼 메소드
   */
  protected BigInteger getBigIntegerValue(Row row, String columnName) {
    Object value = row.getAs(columnName);
    if (value == null) return BigInteger.ZERO;
    return new BigInteger(value.toString());
  }

  protected Integer getIntegerValue(Row row, String columnName) {
    Object value = row.getAs(columnName);
    if (value == null) return 0;
    return Integer.valueOf(value.toString());
  }

  protected Double getDoubleValue(Row row, String columnName) {
    Object value = row.getAs(columnName);
    if (value == null) return 0.0;
    return Double.valueOf(value.toString());
  }

  //확인용
  public void readAndLogS3File(String s3Path) {
    try {
      log.info("S3 파일 읽기 시작: s3://{}/{}", bucketName, s3Path);

      S3Object object = amazonS3Client.getObject(bucketName, s3Path);
      String content = new String(object.getObjectContent().readAllBytes());

      log.info("S3 파일 내용:\n{}", content);

    } catch (Exception e) {
      log.error("S3 파일 읽기 실패: {}", s3Path, e);
    }
  }

  /**
   *  DB 조회 메소드
   */
  /**
   * 외부 콘텐츠 ID -> DB content_id 조회
   */
  protected Integer getContentId(String platformType, String externalContentId) {
    try {
      String sql = "SELECT content_id " +
          "FROM content " +
          "WHERE platform_type_id = ? " +
          "AND external_content_id = ? " +
          "LIMIT 1";

      List<Integer> results = jdbcTemplate.query(sql,
          (rs, rowNum) -> rs.getInt("content_id"),
          platformType,
          externalContentId
      );

      if (results.isEmpty()) {
        log.warn("Content not found for platform={}, externalContentId={}",
            platformType, externalContentId);
        return null;
      }

      return results.get(0);

    } catch (Exception e) {
      log.error("getContentId 매핑 실패: platform={}, externalContentId={}",
          platformType, externalContentId, e);
      return null;
    }
  }

  /**
   * 카테고리명 -> DB category_type_id 조회
   */
  protected Integer getCategoryTypeId(String categoryName) {
    try {
      String sql = "SELECT category_type_id " +
          "FROM category_type " +
          "WHERE category_name = ? " +
          "LIMIT 1";

      List<Integer> results = jdbcTemplate.query(sql,
          (rs, rowNum) -> rs.getInt("category_type_id"),
          categoryName
      );

      if (results.isEmpty()) {
        log.warn("CategoryType not found for categoryName={}",
            categoryName);
        return null;
      }

      return results.get(0);

    } catch (Exception e) {
      log.error("CategoryName 매핑 실패: categoryName={}",
          categoryName, e);
      return null;
    }
  }
  /**
   * 외부 계정 닉네임 ->  PlatformAccount ID 조회
   */
  protected Integer getPlatformAccountId(String platformType, String accountNickname) {
    try {
      String sql = "SELECT platform_account_id FROM platform_account WHERE platform_type_id= ? AND account_nickname = ?";
      return jdbcTemplate.queryForObject(sql, Integer.class, platformType.toUpperCase(), accountNickname);
    } catch (Exception e) {
      log.warn("PlatformAccount 조회 실패: platform={}, accountNickname={}", platformType, accountNickname);
      return null;
    }
  }

  /**
   * 외부 계정 닉네임 ->  PlatformAccount ID 조회
   */
  protected Integer getInfluencerIdByPlatformAccount(Integer platformAccountId) {
    String sql = "SELECT influencer_id FROM platform_account WHERE platform_account_id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, Integer.class, platformAccountId);
    } catch (Exception e) {
      log.error("influencer_id 조회 실패: platformAccountId={}", platformAccountId, e);
      return null;
    }
  }

  // 계정 ID로 닉네임 조회하는 헬퍼 메서드 추가 필요
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