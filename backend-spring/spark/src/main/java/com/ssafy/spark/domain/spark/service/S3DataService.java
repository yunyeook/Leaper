package com.ssafy.spark.domain.spark.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 크롤링 후 row-data에 저장하는 기능 :
 * */
@Service
public class S3DataService {

  //TODO: FILEINFO로 수정하기
  @Autowired
  private AmazonS3Client amazonS3Client;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  /**
   * 콘텐츠와 썸네일을 함께 저장
   */
  public String saveContentWithThumbnail(String platform, String contentType, Integer contentId, String jsonData, byte[] thumbnailData, String mimeType) {
    try {
      // 1. 썸네일 저장
      saveThumbnailImage(platform, contentType, contentId, thumbnailData, mimeType);

      // 2. JSON에 썸네일 정보 추가
      String folderPath = String.format("raw_data/%s/content/%s", platform, contentType.toLowerCase());
      String thumbnailAccessKey = String.format("%s/thumb_%s", folderPath, contentId);
      String updatedJsonData = addThumbnailInfoToJson(jsonData, thumbnailAccessKey, mimeType);

      // 3. JSON 저장
      return saveContentJson(platform, contentType, contentId, updatedJsonData);

    } catch (Exception e) {
      throw new RuntimeException("콘텐츠 저장 실패: " + contentId, e);
    }
  }

  /**
   * 플랫폼 계정 정보와 프로필 이미지를 함께 저장
   */
  public String savePlatformAccount(String platform, String accountId, String jsonData, byte[] profileImageData, String mimeType) {
    try {
      // 1. 프로필 이미지 저장
      String folderPath = String.format("raw_data/%s/platform-account", platform.toLowerCase());
      String profileFileName = String.format("profile_%s", accountId);
      String profileAccessKey = String.format("%s/%s", folderPath, profileFileName);
      uploadFile(profileAccessKey, profileImageData, mimeType);

      // 2. JSON에 프로필 이미지 정보 추가
      String updatedJsonData = addProfileImageInfoToJson(jsonData, profileAccessKey, mimeType);

      // 3. JSON 저장
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String jsonFileName = String.format("account_%s_%s.json", accountId, timestamp);
      String jsonAccessKey = String.format("%s/%s", folderPath, jsonFileName);
      uploadFile(jsonAccessKey, updatedJsonData.getBytes(), "application/json");

      return amazonS3Client.getUrl(bucketName, jsonAccessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("계정 정보 저장 실패: " + accountId, e);
    }
  }

  /**
   * JSON 데이터만 저장 (썸네일 없는 경우)
   */
  public String saveContentJson(String platform, String contentType, Integer contentId, String jsonData) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String folderPath = String.format("raw_data/%s/content/%s", platform, contentType.toLowerCase());
      String fileName = String.format("%s_%s_%s.json", contentType.toLowerCase(), contentId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      uploadFile(accessKey, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("JSON 저장 실패: " + contentId, e);
    }
  }

  /**
   * 썸네일 이미지만 저장
   */
  public String saveThumbnailImage(String platform, String contentType, Integer contentId, byte[] imageData, String mimeType) {
    try {
      String folderPath = String.format("raw_data/%s/content/%s", platform, contentType.toLowerCase());
      String fileName = String.format("thumb_%s", contentId);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      return uploadFile(accessKey, imageData, mimeType);

    } catch (Exception e) {
      throw new RuntimeException("썸네일 저장 실패: " + contentId, e);
    }
  }

  // ========== JSON 조작 유틸리티 ==========

  /**
   * JSON에 썸네일 정보 추가 (File 엔티티 형태)
   */
  private String addThumbnailInfoToJson(String jsonData, String accessKey, String mimeType) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonData);

      ObjectNode thumbnailInfo = objectMapper.createObjectNode();
      thumbnailInfo.put("accessKey", accessKey);
      thumbnailInfo.put("contentType", mimeType);

      ((ObjectNode) jsonNode).set("thumbnailInfo", thumbnailInfo);

      return objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(jsonNode);

    } catch (Exception e) {
      throw new RuntimeException("JSON 썸네일 정보 추가 실패", e);
    }
  }

  /**
   * JSON에 프로필 이미지 정보 추가 (File 엔티티 형태)
   */
  private String addProfileImageInfoToJson(String jsonData, String accessKey, String mimeType) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonData);

      ObjectNode profileImageInfo = objectMapper.createObjectNode();
      profileImageInfo.put("accessKey", accessKey);
      profileImageInfo.put("contentType", mimeType);

      ((ObjectNode) jsonNode).set("profileImageInfo", profileImageInfo);

      // 이 한 줄로 pretty print 가능!
      return objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(jsonNode);

    } catch (Exception e) {
      throw new RuntimeException("JSON 프로필 이미지 정보 추가 실패", e);
    }
  }

  // ========== 파일 관리 기능 ==========

  /**
   * 파일 존재 여부 확인
   */
  public boolean doesFileExist(String accessKey) {
    try {
      return amazonS3Client.doesObjectExist(bucketName, accessKey);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 파일 삭제
   */
  public void deleteFile(String accessKey) {
    try {
      amazonS3Client.deleteObject(bucketName, accessKey);
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 삭제 실패: " + accessKey, e);
    }
  }

  /**
   * 파일 메타데이터 조회
   */
  public ObjectMetadata getFileMetadata(String accessKey) {
    try {
      return amazonS3Client.getObjectMetadata(bucketName, accessKey);
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 메타데이터 조회 실패: " + accessKey, e);
    }
  }

  /**
   * 파일 스트림으로 읽기 (큰 파일용)
   */
  public InputStream getFileStream(String accessKey) {
    try {
      S3Object object = amazonS3Client.getObject(bucketName, accessKey);
      return object.getObjectContent();
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 스트림 읽기 실패: " + accessKey, e);
    }
  }

  // ========== 내부 유틸리티 메소드 ==========

  /**
   * 파일 업로드 공통 메소드
   */
  private String uploadFile(String accessKey, byte[] content, String contentType) {
    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(content.length);
      metadata.setContentType(contentType);

      if (contentType.startsWith("image/")) {
        metadata.setCacheControl("max-age=31536000"); // 1년 캐시
      }

      PutObjectRequest request = new PutObjectRequest(
          bucketName,
          accessKey,
          new ByteArrayInputStream(content),
          metadata
      );

      amazonS3Client.putObject(request);
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("S3 파일 업로드 실패: " + accessKey, e);
    }
  }
}