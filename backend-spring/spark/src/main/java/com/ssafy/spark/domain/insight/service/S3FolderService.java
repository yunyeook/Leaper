package com.ssafy.spark.domain.insight.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class S3FolderService {

  @Autowired
  private AmazonS3 amazonS3;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  /**
   * S3 경로(폴더) 존재 여부 확인
   */
  public boolean doesFolderExist(String folderPath) {
    try {
      // 폴더 경로는 반드시 '/'로 끝나야 함
      String normalizedPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";

      // 해당 경로의 객체 목록을 조회 (최대 1개만)
      ListObjectsV2Request request = new ListObjectsV2Request()
          .withBucketName(bucketName)
          .withPrefix(normalizedPath)
          .withMaxKeys(1);

      ListObjectsV2Result result = amazonS3.listObjectsV2(request);

      // 객체가 하나라도 있으면 폴더가 존재한다고 판단
      boolean exists = !result.getObjectSummaries().isEmpty();

      log.info("S3 폴더 존재 확인: {} -> {}", normalizedPath, exists);
      return exists;

    } catch (Exception e) {
      log.error("S3 폴더 존재 확인 실패: {}, 오류: {}", folderPath, e.getMessage());
      return false;
    }
  }

  /**
   * S3에 빈 폴더 생성 (빈 객체 생성으로 폴더 표시)
   */
  public void createFolder(String folderPath) {
    try {
      // 폴더 경로는 반드시 '/'로 끝나야 함
      String normalizedPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";

      // 빈 내용으로 객체 생성 (폴더 역할)
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(0);

      InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
      PutObjectRequest request = new PutObjectRequest(bucketName, normalizedPath, emptyContent, metadata);

      amazonS3.putObject(request);
      log.info("S3 폴더 생성 완료: {}", normalizedPath);

    } catch (Exception e) {
      log.error("S3 폴더 생성 실패: {}, 오류: {}", folderPath, e.getMessage());
      throw new RuntimeException("S3 폴더 생성 실패", e);
    }
  }

  /**
   * 필요한 경우 폴더 생성
   */
  public void ensureFolderExists(String folderPath) {
    if (!doesFolderExist(folderPath)) {
      log.info("폴더가 존재하지 않아 생성합니다: {}", folderPath);
      createFolder(folderPath);
    } else {
      log.info("폴더가 이미 존재합니다: {}", folderPath);
    }
  }

  /**
   * 날짜별 폴더 구조 생성
   */
  public void createDateFolders(String platform, LocalDate date) {
    String basePath = String.format("raw_data/%s", platform);
    String datePath = String.format("%s/%04d/%02d/%02d",
        basePath, date.getYear(), date.getMonthValue(), date.getDayOfMonth());

    // 필요한 하위 폴더들
    String[] subFolders = {
        "platform_account",
        "content",
        "comment",
        "profile_images",
        "content_thumbnail_images"
    };

    for (String subFolder : subFolders) {
      String fullPath = String.format("%s/%s/%04d/%02d/%02d",
          basePath, subFolder, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
      ensureFolderExists(fullPath);
    }

    log.info("날짜별 폴더 구조 생성 완료: {}", datePath);
  }


}