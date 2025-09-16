package com.ssafy.spark.domain.spark.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
  protected ObjectMapper objectMapper;  // S3 JSON 저장용 추가

  @Autowired
  protected AmazonS3Client amazonS3Client;  // S3 저장용 추가

  @Value("${cloud.aws.s3.bucket}")
  protected String bucketName;


  /**
   * 특정 날짜의 콘텐츠 데이터 읽기 : 주로 오늘 날짜 데이터를 읽음.
   */
  protected Dataset<Row> readS3ContentDataByDate(String platformType, LocalDate targetDate) {
    try {
      // 특정 날짜 폴더의 콘텐츠 데이터 읽기
      String dateFolder = targetDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String s3Path = String.format("s3a://%s/raw_data/%s/content/%s/*.json", bucketName,platformType, dateFolder);

      return sparkSession.read()
          .option("multiline", "true")
          .json(s3Path);

    } catch (Exception e) {
      log.warn("S3 특정날짜 콘텐츠 데이터 읽기 실패: platform={}, date={}",
          platformType, targetDate, e);
      // 빈 DataFrame 반환
      return sparkSession.emptyDataFrame();
    }
  }

  /**
   * 전체 계정 데이터 읽기
   */
  protected Dataset<Row> readS3AccountData(String platformType) {
    try {
      // 모든 날짜 폴더의 계정 데이터 읽기
      String s3Path = String.format("s3a://%s/raw_data/%s/platform-account/*.json",bucketName, platformType);

      return sparkSession.read()
          .option("multiline", "true")
          .json(s3Path);

    } catch (Exception e) {
      log.error("S3 전체 계정 데이터 읽기 실패: platform={}", platformType, e);
      return sparkSession.emptyDataFrame();
    }
  }

  /**
   * PlatformAccount ID 조회
   */
  protected Integer getPlatformAccountId(String platform, String externalAccountId) {
    try {
      String sql = "SELECT platform_account_id FROM platform_account WHERE platform_type_id= ? AND external_account_id = ?";
      return jdbcTemplate.queryForObject(sql, Integer.class, platform.toUpperCase(), externalAccountId);
    } catch (Exception e) {
      log.warn("PlatformAccount 조회 실패: platform={}, externalAccountId={}", platform, externalAccountId);
      return null;
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
}