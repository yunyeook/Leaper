package com.ssafy.spark.domain.analysis.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.spark.domain.business.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 크롤링 후 row-data에 저장하는 기능 :
 */
@Service
public class S3DataService {

  // TODO: FILEINFO로 수정하기
  @Autowired
  private AmazonS3Client amazonS3Client;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FileService fileService;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  /**
   * 콘텐츠와 썸네일을 함께 저장
   */
  public String saveContentWithThumbnail(String platform, String contentType, Integer contentId, String jsonData,
      byte[] thumbnailData, String mimeType) {
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
  public String savePlatformAccount(String platform, String accountId, String jsonData, byte[] profileImageData,
      String mimeType) {
    try {
      // 1. 프로필 이미지 저장
      String folderPath = String.format("raw_data/%s/platform_account", platform.toLowerCase());
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
  public String saveThumbnailImage(String platform, String contentType, Integer contentId, byte[] imageData,
      String mimeType) {
    try {
      String folderPath = String.format("raw_data/%s/content/%s", platform, contentType.toLowerCase());
      String fileName = String.format("thumb_%s", contentId);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      return uploadFile(accessKey, imageData, mimeType);

    } catch (Exception e) {
      throw new RuntimeException("썸네일 저장 실패: " + contentId, e);
    }
  }

  // ========== YouTube 크롤링 데이터 저장 메서드 ==========

  /**
   * YouTube 채널 정보 저장 (날짜별 경로)
   * 경로: raw_data/youtube/platform_account/yyyy/MM/dd/
   */
  public String saveYouTubeChannelInfo(String externalAccountId, String jsonData) {
    try {
      LocalDateTime now = LocalDateTime.now();
      String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = now.format(DateTimeFormatter.ofPattern("yyMMdd_HH_mm_ss_SSS"));

      String folderPath = String.format("raw_data/youtube/platform_account/%s", datePath);
      String fileName = String.format("%s_%s.json", externalAccountId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      uploadFile(accessKey, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("YouTube 채널 정보 저장 실패", e);
    }
  }

  /**
   * YouTube 비디오 정보 저장 (날짜별 경로)
   * 경로: raw_data/youtube/content/yyyy/MM/dd/
   */
  public String saveYouTubeVideoInfo(String videoId, String jsonData) {
    try {
      LocalDateTime now = LocalDateTime.now();
      String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = now.format(DateTimeFormatter.ofPattern("yyMMdd_HH_mm_ss_SSS"));

      String folderPath = String.format("raw_data/youtube/content/%s", datePath);
      String fileName = String.format("%s_%s.json", videoId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      uploadFile(accessKey, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("YouTube 비디오 정보 저장 실패: " + videoId, e);
    }
  }

  /**
   * YouTube 댓글 정보 저장 (날짜별 경로)
   * 경로: raw_data/youtube/comment/yyyy/MM/dd/
   */
  public String saveYouTubeComments(String videoId, String jsonData, Integer contentId) {
    try {
      LocalDateTime now = LocalDateTime.now();
      String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = String.valueOf(now.toEpochSecond(java.time.ZoneOffset.UTC) * 1000 + now.getNano() / 1000000);

      String folderPath = String.format("raw_data/youtube/comment/%s", datePath);
      String fileName = String.format("content_%s_%s.json", contentId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      uploadFile(accessKey, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("YouTube 댓글 정보 저장 실패: " + videoId, e);
    }
  }

  /**
   * 여러 비디오를 배치로 저장
   */
  public String saveYouTubeVideoBatch(String batchId, String jsonData) {
    try {
      LocalDateTime now = LocalDateTime.now();
      String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      String timestamp = now.format(DateTimeFormatter.ofPattern("yyMMdd_HH_mm_ss_SSS"));

      String folderPath = String.format("raw_data/youtube/content/%s", datePath);
      String fileName = String.format("batch_%s_%s.json", batchId, timestamp);
      String accessKey = String.format("%s/%s", folderPath, fileName);

      uploadFile(accessKey, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("YouTube 비디오 배치 저장 실패", e);
    }
  }

  /**
   * YouTube 썸네일 이미지를 URL에서 다운로드하여 S3에 저장
   * 경로:
   * raw_data/youtube/content_thumbnail_images/{externalContentId}_{timestamp}.jpg
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
          metadata);

      amazonS3Client.putObject(request);
      return amazonS3Client.getUrl(bucketName, accessKey).toString();

    } catch (Exception e) {
      throw new RuntimeException("S3 파일 업로드 실패: " + accessKey, e);
    }
  }
}