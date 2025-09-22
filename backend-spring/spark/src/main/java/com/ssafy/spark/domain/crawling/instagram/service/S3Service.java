package com.ssafy.spark.domain.crawling.instagram.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

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

  public String uploadProfileData(String jsonData, String externalAccountId) {
    try {
      LocalDate now = LocalDate.now();
      String key = String.format("raw_data/instagram/platform_account/%d/%02d/%02d/%s_%d.json",
          now.getYear(),
          now.getMonthValue(),
          now.getDayOfMonth(),
          externalAccountId,
          System.currentTimeMillis());

      // UTF-8 바이트로 변환
      byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json; charset=utf-8");
      metadata.setContentLength(jsonBytes.length); // 바이트 길이로 설정
      metadata.setContentEncoding("utf-8");

      // ByteArrayInputStream 사용 (자동으로 닫힘)
      try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
        amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
      }

      log.info("S3 업로드 완료: {} (크기: {} bytes)", key, jsonBytes.length);
      return key;

    } catch (Exception e) {
      log.error("S3 업로드 실패: ", e);
      throw new RuntimeException("S3 업로드 실패", e);
    }
  }
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

  public String uploadContentData(String jsonData, String key) {
    try {
      byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json; charset=utf-8");
      metadata.setContentLength(jsonBytes.length);
      metadata.setContentEncoding("utf-8");

      try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
        amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));
      }

      log.info("콘텐츠 데이터 S3 업로드 완료: {} (크기: {} bytes)", key, jsonBytes.length);
      return key;

    } catch (Exception e) {
      log.error("콘텐츠 데이터 S3 업로드 실패: ", e);
      throw new RuntimeException("콘텐츠 데이터 S3 업로드 실패", e);
    }
  }
}