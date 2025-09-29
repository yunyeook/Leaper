package com.ssafy.spark.domain.crawling.instagram.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingDataSaveToS3Service {

  private final SparkSession sparkSession;
  private final ObjectMapper objectMapper;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  @Value("${cloud.aws.credentials.accessKey}")
  private String accessKey;

  @Value("${cloud.aws.credentials.secretKey}")
  private String secretKey;

  @Value("${cloud.aws.region.static}")
  private String region;

  private AmazonS3 amazonS3Client;

  @PostConstruct
  public void initializeS3Client() {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    this.amazonS3Client = AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(region)
        .build();
  }

  /**
   * 크롤링한 프로필 데이터 저장
   *
   * 폴더 구조:
   * raw_data/
   *   ├── json/{instagram}/platform_account/{yyyy}/{mm}/{dd}/{id}_{timestamp}.json
   *   └── parquet/{instagram}/platform_account/{yyyy}/{mm}/{dd}/{id}_{timestamp}.parquet
   */
  public Map<String, String> uploadProfileData(String jsonData,String platformType, String externalAccountId) {
    try {
      LocalDate now = LocalDate.now();
      String dateFolder = String.format("%d/%02d/%02d",
          now.getYear(), now.getMonthValue(), now.getDayOfMonth());
      String filename = String.format("%s_%d", externalAccountId, System.currentTimeMillis());

      Map<String, String> uploadedPaths = new HashMap<>();

      // 1. JSON 저장 - json 폴더
      String jsonKey = String.format("raw_data/json/%s/platform_account/%s/%s.json",platformType, dateFolder, filename);
      uploadJsonData(jsonData, jsonKey);
      uploadedPaths.put("json", jsonKey);

      // 2. Parquet 저장 - parquet 폴더
      String parquetKey = String.format("raw_data/parquet/%s/platform_account/%s/%s",platformType, dateFolder, filename);
      uploadParquetData(jsonData, parquetKey);
      uploadedPaths.put("parquet", parquetKey);

      log.info("프로필 데이터 업로드 완료 (JSON + Parquet 분리): {}", filename);
      return uploadedPaths;

    } catch (Exception e) {
      log.error("프로필 데이터 업로드 실패: ", e);
      throw new RuntimeException("프로필 데이터 업로드 실패", e);
    }
  }

  /**
   * 컨텐츠 데이터를 JSON과 Parquet으로 분리된 폴더에 저장
   *
   * 폴더 구조:
   * raw_data/
   *   ├── json/instagram/content/{yyyy}/{mm}/{dd}/{externalContentId}_{timestamp}.json
   *   └── parquet/instagram/content/{yyyy}/{mm}/{dd}/{externalContentId}_{timestamp}.parquet
   */
  public Map<String, String> uploadContentData(String jsonData,String platformType,String externalContentId) {
    try {
      LocalDate now = LocalDate.now();
      String dateFolder = String.format("%d/%02d/%02d",
          now.getYear(), now.getMonthValue(), now.getDayOfMonth());
      String filename = String.format("%s_%d", externalContentId, System.currentTimeMillis());

      Map<String, String> uploadedPaths = new HashMap<>();

      // 1. JSON 저장
      String jsonKey = String.format("raw_data/json/%s/content/%s/%s.json",platformType, dateFolder, filename);
      uploadJsonData(jsonData, jsonKey);
      uploadedPaths.put("json", jsonKey);

      // 2. Parquet 저장
      String parquetKey = String.format("raw_data/parquet/%s/content/%s/%s",platformType, dateFolder, filename);
      uploadParquetData(jsonData, parquetKey);
      uploadedPaths.put("parquet", parquetKey);

      log.info("컨텐츠 데이터 업로드 완료 (JSON + Parquet 분리): {}", filename);
      return uploadedPaths;

    } catch (Exception e) {
      log.error("컨텐츠 데이터 업로드 실패: ", e);
      throw new RuntimeException("컨텐츠 데이터 업로드 실패", e);
    }
  }

  /**
   * 크롤링한 댓글 데이터 저장
   *
   * 폴더 구조:
   * raw_data/
   *   ├── json/instagram/comment/{yyyy}/{mm}/{dd}/content_{contentId}_{timestamp}.json
   *   └── parquet/instagram/comment/{yyyy}/{mm}/{dd}/content_{contentId}_{timestamp}.parquet
   */
  public Map<String, String> uploadCommentData(String jsonData, String platformType,String contentId) {
    try {
      LocalDate now = LocalDate.now();
      String dateFolder = String.format("%d/%02d/%02d",
          now.getYear(), now.getMonthValue(), now.getDayOfMonth());
      String filename = String.format("content_%s_%d", contentId, System.currentTimeMillis());

      Map<String, String> uploadedPaths = new HashMap<>();

      // 1. JSON 저장
      String jsonKey = String.format("raw_data/%s/comment/%s/%s.json",platformType,
//          String jsonKey = String.format("raw_data/json/%s/comment/%s/%s.json",platformType,
          dateFolder, filename);
      uploadJsonData(jsonData, jsonKey);
      uploadedPaths.put("json", jsonKey);

      // 2. Parquet 저장
      String parquetKey = String.format("raw_data/parquet/instagram/comment/%s/%s",
          dateFolder, filename);
      uploadParquetData(jsonData, parquetKey);
      uploadedPaths.put("parquet", parquetKey);

      log.info("댓글 데이터 업로드 완료 (JSON + Parquet 분리): {}", filename);
      return uploadedPaths;

    } catch (Exception e) {
      log.error("댓글 데이터 업로드 실패: ", e);
      throw new RuntimeException("댓글 데이터 업로드 실패", e);
    }
  }

  /**
   * 이미지 파일 업로드
   */
  public String uploadImageFile(byte[] imageBytes, String key, String contentType) {
    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(contentType);
      metadata.setContentLength(imageBytes.length);

      InputStream inputStream = new ByteArrayInputStream(imageBytes);
      amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));

      log.info("이미지 S3 업로드 완료: {}", key);
      return key;

    } catch (Exception e) {
      log.error("이미지 S3 업로드 실패: ", e);
      throw new RuntimeException("이미지 S3 업로드 실패", e);
    }
  }

  // ========== Private Helper Methods ==========

  /**
   * JSON 데이터를 S3에 업로드
   */
  private void uploadJsonData(String jsonData, String key) {
    try {
      byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json; charset=utf-8");
      metadata.setContentLength(jsonBytes.length);
      metadata.setContentEncoding("utf-8");

      try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
        amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
      }

      log.debug("JSON 업로드 완료: {} (크기: {} bytes)", key, jsonBytes.length);

    } catch (Exception e) {
      log.error("JSON 업로드 실패: {}", key, e);
      throw new RuntimeException("JSON 업로드 실패: " + key, e);
    }
  }

  /**
   * JSON 데이터를 Parquet으로 변환하여 S3에 업로드
   */
  private void uploadParquetData(String jsonData, String key) {
    File tempJsonFile = null;
    try {
      // 1. 임시 JSON 파일 생성
      tempJsonFile = Files.createTempFile("spark-temp-", ".json").toFile();
      Files.write(tempJsonFile.toPath(), jsonData.getBytes(StandardCharsets.UTF_8));

      // 2. Spark로 JSON 읽기
      Dataset<Row> df = sparkSession.read()
          .option("multiline", "true")
          .json(tempJsonFile.getAbsolutePath());

      // 3. S3에 Parquet으로 저장
      String s3Path = String.format("s3a://%s/%s", bucketName, key);
      df.write()
          .mode(SaveMode.Overwrite)
          .option("compression", "snappy")
          .parquet(s3Path);

      log.debug("Parquet 업로드 완료: {}", key);

    } catch (Exception e) {
      log.error("Parquet 업로드 실패: {}", key, e);
      throw new RuntimeException("Parquet 업로드 실패: " + key, e);
    } finally {
      // 4. 임시 파일 삭제
      if (tempJsonFile != null && tempJsonFile.exists()) {
        tempJsonFile.delete();
      }
    }
  }
}