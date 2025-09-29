package com.ssafy.spark.domain.insight.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.spark.domain.business.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 크롤링 후 row-data에 저장하는 기능 :
 * */
@Service
public class ReadToS3DataService {

  //TODO: FILEINFO로 수정하기
  @Autowired
  private AmazonS3Client amazonS3Client;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FileService fileService;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;


  // ========== YouTube 크롤링 데이터 저장 메서드 ==========


  /**
   * YouTube 썸네일 이미지를 URL에서 다운로드하여 S3에 저장
   * 경로: raw_data/youtube/content_thumbnail_images/{externalContentId}_{timestamp}.jpg
   * File 테이블에도 저장
   */
  public String saveYouTubeThumbnailFromUrl(String username, String externalContentId, String thumbnailUrl) {
    try {
      // 썸네일 URL에서 이미지 다운로드
      URL url = new URL(thumbnailUrl);
      InputStream inputStream = url.openStream();
      byte[] imageData = inputStream.readAllBytes();
      inputStream.close();

      // 파일 경로 및 이름 생성
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HH_mm_ss_SSS"));
      String folderPath = "raw_data/youtube/content_thumbnail_images";
      String fileName = String.format("%s_%s.jpg", externalContentId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      // S3에 업로드
      uploadFile(accessKey, imageData, "image/jpeg");

      // File 테이블에 저장
      try {
        fileService.createFile(accessKey, "image/jpeg");
        // 로그는 FileService에서 처리
      } catch (Exception fileError) {
        System.err.println("File 테이블 저장 실패: " + accessKey + " - " + fileError.getMessage());
      }

      return accessKey; // S3 accessKey 반환

    } catch (Exception e) {
      throw new RuntimeException("YouTube 썸네일 저장 실패: " + externalContentId, e);
    }
  }

  /**
   * 프로필 이미지를 URL에서 다운로드하여 S3에 저장
   * 경로: raw_data/youtube/profile_images/{username}/{username}_{timestamp}.jpg
   * File 테이블에도 저장하고 File ID 반환
   */
  public ProfileImageSaveResult saveProfileImageFromUrl(String username, String profileImageUrl) {
    try {
      // 프로필 이미지 URL에서 이미지 다운로드
      URL url = new URL(profileImageUrl);
      InputStream inputStream = url.openStream();
      byte[] imageData = inputStream.readAllBytes();
      inputStream.close();

      // 파일 경로 및 이름 생성
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HH_mm_ss_SSS"));
      String folderPath = String.format("raw_data/youtube/profile_images/%s", username);
      String fileName = String.format("%s_%s.jpg", username, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      // S3에 업로드
      uploadFile(accessKey, imageData, "image/jpeg");

      // File 테이블에 저장하고 ID 받기
      Integer fileId = null;
      try {
        var fileResponse = fileService.createFile(accessKey, "image/jpeg");
        fileId = fileResponse.getId();
      } catch (Exception fileError) {
        fileError.printStackTrace();
      }
      return new ProfileImageSaveResult(accessKey, fileId);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("프로필 이미지 저장 실패: " + username, e);
    }
  }

  /**
   * 프로필 이미지 저장 결과를 담는 클래스
   */
  public static class ProfileImageSaveResult {
    private final String accessKey;
    private final Integer fileId;

    public ProfileImageSaveResult(String accessKey, Integer fileId) {
      this.accessKey = accessKey;
      this.fileId = fileId;
    }

    public String getAccessKey() {
      return accessKey;
    }

    public Integer getFileId() {
      return fileId;
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

  // ========== 데이터 조회 기능 ==========

  /**
   * S3에서 JSON 파일 내용 읽기
   */
  public String getFileContent(String accessKey) {
    try {
      S3Object object = amazonS3Client.getObject(bucketName, accessKey);
      InputStream inputStream = object.getObjectContent();
      return new String(inputStream.readAllBytes());
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 읽기 실패: " + accessKey, e);
    }
  }

  /**
   * 특정 날짜의 YouTube 채널 정보 파일 목록 조회
   */
  public java.util.List<String> listYouTubeChannelFiles(String date) {
    String folderPath = String.format("raw_data/youtube/platform_account/%s/", date.replace("-", "/"));
    return listFilesInFolder(folderPath);
  }

  /**
   * 특정 날짜의 YouTube 비디오 파일 목록 조회
   */
  public java.util.List<String> listYouTubeVideoFiles(String date) {
    String folderPath = String.format("raw_data/youtube/content/%s/", date.replace("-", "/"));
    return listFilesInFolder(folderPath);
  }

  /**
   * 특정 날짜의 YouTube 댓글 파일 목록 조회
   */
  public java.util.List<String> listYouTubeCommentFiles(String date) {
    String folderPath = String.format("raw_data/youtube/comment/%s/", date.replace("-", "/"));
    return listFilesInFolder(folderPath);
  }

  /**
   * 폴더 내 파일 목록 조회
   */
  private java.util.List<String> listFilesInFolder(String folderPath) {
    try {
      ListObjectsV2Request request = new ListObjectsV2Request()
          .withBucketName(bucketName)
          .withPrefix(folderPath);

      ListObjectsV2Result result = amazonS3Client.listObjectsV2(request);
      return result.getObjectSummaries().stream()
          .map(S3ObjectSummary::getKey)
          .filter(key -> key.endsWith(".json"))
          .collect(java.util.stream.Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("S3 폴더 목록 조회 실패: " + folderPath, e);
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