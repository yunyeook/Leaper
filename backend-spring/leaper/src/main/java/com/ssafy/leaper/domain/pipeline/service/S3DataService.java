package com.ssafy.leaper.domain.pipeline.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3DataService {

  @Autowired
  private AmazonS3Client amazonS3Client;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  // ========== 플랫폼별 Content 저장 메소드 ==========

  /**
   * 인스타그램 콘텐츠 저장 (JSON + 썸네일)
   */
  public String saveInstagramContent(String contentType, String contentId, String jsonData, byte[] thumbnailData, String thumbnailExtension) {
    String folderPath = String.format("raw-data/instagram/content/%s", contentType.toLowerCase());
    return saveContentWithThumbnail(folderPath, contentType, contentId, jsonData, thumbnailData, thumbnailExtension);
  }

  /**
   * 유튜브 콘텐츠 저장 (JSON + 썸네일)
   */
  public String saveYoutubeContent(String contentType, String contentId, String jsonData, byte[] thumbnailData, String thumbnailExtension) {
    String folderPath = String.format("raw-data/youtube/content/%s", contentType.toLowerCase());
    return saveContentWithThumbnail(folderPath, contentType, contentId, jsonData, thumbnailData, thumbnailExtension);
  }

  /**
   * 네이버 블로그 콘텐츠 저장 (JSON + 썸네일)
   */
  public String saveNaverBlogContent(String contentId, String jsonData, byte[] thumbnailData, String thumbnailExtension) {
    String folderPath = "raw-data/naver-blog/content/post";
    return saveContentWithThumbnail(folderPath, "POST", contentId, jsonData, thumbnailData, thumbnailExtension);
  }

  /**
   * 콘텐츠와 썸네일을 함께 저장하는 통합 메소드
   */
  private String saveContentWithThumbnail(String folderPath, String contentType, String contentId, String jsonData, byte[] thumbnailData, String thumbnailExtension) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      // 1. 썸네일 파일 저장
      String thumbnailFileName = String.format("thumb_%s.%s", contentId, thumbnailExtension);
      String thumbnailS3Key = String.format("%s/%s", folderPath, thumbnailFileName);
      String thumbnailUrl = uploadFile(thumbnailS3Key, thumbnailData, getSnsImageContentType(thumbnailExtension));

      // 2. JSON에 썸네일 정보 추가
      String updatedJsonData = addThumbnailInfoToJson(jsonData, thumbnailS3Key, thumbnailFileName, thumbnailExtension, thumbnailData.length);

      // 3. JSON 파일 저장
      String jsonFileName = String.format("%s_%s_%s.json", contentType.toLowerCase(), contentId, timestamp);
      String jsonS3Key = String.format("%s/%s", folderPath, jsonFileName);
      uploadFile(jsonS3Key, updatedJsonData.getBytes(), "application/json");

      return amazonS3Client.getUrl(bucketName, jsonS3Key).toString();

    } catch (Exception e) {
      throw new RuntimeException("콘텐츠 저장 실패: " + contentId, e);
    }
  }

  /**
   * JSON에 썸네일 정보 추가
   */
  private String addThumbnailInfoToJson(String jsonData, String s3Key, String originalFileName, String extension, long fileSize) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonData);

      // 썸네일 정보 객체 생성
      String thumbnailInfo = String.format("""
        {
          "s3Key": "%s",
          "originalFileName": "%s",
          "contentType": "%s",
          "fileSize": %d
        }
        """, s3Key, originalFileName, getSnsImageContentType(extension), fileSize);

      JsonNode thumbnailInfoNode = objectMapper.readTree(thumbnailInfo);

      // JSON에 thumbnailInfo 필드 추가
      ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).set("thumbnailInfo", thumbnailInfoNode);

      return objectMapper.writeValueAsString(jsonNode);

    } catch (Exception e) {
      throw new RuntimeException("JSON 썸네일 정보 추가 실패", e);
    }
  }

  // ========== Platform Account 저장 메소드 ==========

  /**
   * 인스타그램 계정 정보 저장
   */
  public String saveInstagramAccount(String accountId, String jsonData, byte[] profileImageData, String imageExtension) {
    String folderPath = "raw-data/instagram/platform-account";
    return savePlatformAccount(folderPath, accountId, jsonData, profileImageData, imageExtension);
  }

  /**
   * 유튜브 채널 정보 저장
   */
  public String saveYoutubeAccount(String channelId, String jsonData, byte[] channelImageData, String imageExtension) {
    String folderPath = "raw-data/youtube/platform-account";
    return savePlatformAccount(folderPath, channelId, jsonData, channelImageData, imageExtension);
  }

  /**
   * 네이버 블로그 계정 정보 저장
   */
  public String saveNaverBlogAccount(String bloggerId, String jsonData, byte[] profileImageData, String imageExtension) {
    String folderPath = "raw-data/naver-blog/platform-account";
    return savePlatformAccount(folderPath, bloggerId, jsonData, profileImageData, imageExtension);
  }

  /**
   * 플랫폼 계정 정보와 프로필 이미지를 함께 저장
   */
  private String savePlatformAccount(String folderPath, String accountId, String jsonData, byte[] profileImageData, String imageExtension) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      // 1. 프로필 이미지 저장
      String profileImageFileName = String.format("profile_%s.%s", accountId, imageExtension);
      String profileImageS3Key = String.format("%s/%s", folderPath, profileImageFileName);
      uploadFile(profileImageS3Key, profileImageData, getSnsImageContentType(imageExtension));

      // 2. JSON에 프로필 이미지 정보 추가
      String updatedJsonData = addProfileImageInfoToJson(jsonData, profileImageS3Key, profileImageFileName, imageExtension, profileImageData.length);

      // 3. JSON 파일 저장
      String jsonFileName = String.format("account_%s_%s.json", accountId, timestamp);
      String jsonS3Key = String.format("%s/%s", folderPath, jsonFileName);
      uploadFile(jsonS3Key, updatedJsonData.getBytes(), "application/json");

      return amazonS3Client.getUrl(bucketName, jsonS3Key).toString();

    } catch (Exception e) {
      throw new RuntimeException("계정 정보 저장 실패: " + accountId, e);
    }
  }

  /**
   * JSON에 프로필 이미지 정보 추가
   */
  private String addProfileImageInfoToJson(String jsonData, String s3Key, String originalFileName, String extension, long fileSize) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonData);

      // 프로필 이미지 정보 객체 생성
      String profileImageInfo = String.format("""
        {
          "s3Key": "%s",
          "originalFileName": "%s",
          "contentType": "%s",
          "fileSize": %d
        }
        """, s3Key, originalFileName, getSnsImageContentType(extension), fileSize);

      JsonNode profileImageInfoNode = objectMapper.readTree(profileImageInfo);

      // JSON에 profileImageInfo 필드 추가
      ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).set("profileImageInfo", profileImageInfoNode);

      return objectMapper.writeValueAsString(jsonNode);

    } catch (Exception e) {
      throw new RuntimeException("JSON 프로필 이미지 정보 추가 실패", e);
    }
  }

  // ========== 개별 파일 저장 메소드 ==========

  /**
   * JSON 데이터만 저장 (썸네일 없는 경우)
   */
  public String saveContentJson(String platform, String contentType, String contentId, String jsonData) {
    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String folderPath = String.format("raw-data/%s/content/%s", platform, contentType.toLowerCase());
      String fileName = String.format("%s_%s_%s.json", contentType.toLowerCase(), contentId, timestamp);
      String keyName = String.format("%s/%s", folderPath, fileName);

      uploadFile(keyName, jsonData.getBytes(), "application/json");
      return amazonS3Client.getUrl(bucketName, keyName).toString();

    } catch (Exception e) {
      throw new RuntimeException("JSON 저장 실패: " + contentId, e);
    }
  }

  /**
   * 썸네일 이미지만 저장
   */
  public String saveThumbnailImage(String platform, String contentType, String contentId, byte[] imageData, String imageExtension) {
    try {
      String folderPath = String.format("raw-data/%s/content/%s", platform, contentType.toLowerCase());
      String fileName = String.format("thumb_%s.%s", contentId, imageExtension);
      String keyName = String.format("%s/%s", folderPath, fileName);

      return uploadFile(keyName, imageData, getSnsImageContentType(imageExtension));

    } catch (Exception e) {
      throw new RuntimeException("썸네일 저장 실패: " + contentId, e);
    }
  }

  // ========== 기존 호환성 메소드들 (Deprecated) ==========

  /**
   * @deprecated 새로운 saveInstagramContent 사용 권장
   */
  @Deprecated
  public String saveInstagramPost(String jsonData) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String fileName = String.format("instagram_posts_legacy_%s.json", timestamp);
    return saveJsonData("raw-data/instagram/content/post", fileName, jsonData);
  }

  /**
   * @deprecated 새로운 saveYoutubeContent 사용 권장
   */
  @Deprecated
  public String saveYoutubeVideoInfo(String jsonData) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String fileName = String.format("youtube_videos_legacy_%s.json", timestamp);
    return saveJsonData("raw-data/youtube/content/video-long", fileName, jsonData);
  }

  /**
   * @deprecated 새로운 saveInstagramAccount 사용 권장
   */
  @Deprecated
  public String saveInstagramImage(String postId, byte[] imageData, String imageType) {
    String fileName = String.format("%s.%s", postId, imageType);
    return saveImageFile("raw-data/instagram/content/post", fileName, imageData, imageType);
  }

  /**
   * @deprecated 새로운 saveYoutubeAccount 사용 권장
   */
  @Deprecated
  public String saveYoutubeThumbnail(String videoId, byte[] imageData) {
    String fileName = String.format("%s_thumbnail.jpg", videoId);
    return saveImageFile("raw-data/youtube/content/video-long", fileName, imageData, "jpg");
  }

  // ========== 일반 파일 저장/읽기 메소드 ==========

  /**
   * JSON 데이터 저장
   */
  public String saveJsonData(String folderPath, String fileName, String jsonData) {
    String keyName = String.format("%s/%s", folderPath, fileName);
    return uploadFile(keyName, jsonData.getBytes(), "application/json");
  }

  /**
   * JSON 데이터 읽기
   */
  public String readJsonData(String keyName) {
    try {
      S3Object object = amazonS3Client.getObject(bucketName, keyName);
      return new String(object.getObjectContent().readAllBytes());
    } catch (Exception e) {
      throw new RuntimeException("S3 JSON 데이터 읽기 실패: " + keyName, e);
    }
  }

  /**
   * 이미지 파일 저장 (확장자 자동 감지)
   */
  public String saveImageFile(String folderPath, String fileName, byte[] imageData, String imageType) {
    String keyName = String.format("%s/%s", folderPath, fileName);
    String contentType = getSnsImageContentType(imageType);
    return uploadFile(keyName, imageData, contentType);
  }

  /**
   * MultipartFile 이미지 저장
   */
  public String saveImageFile(String folderPath, String fileName, MultipartFile file) throws IOException {
    String keyName = String.format("%s/%s", folderPath, fileName);
    String contentType = file.getContentType() != null ? file.getContentType() :
        getSnsImageContentType(getFileExtension(fileName));
    return uploadFile(keyName, file.getBytes(), contentType);
  }

  /**
   * 이미지 파일 읽기
   */
  public byte[] readImageFile(String keyName) {
    try {
      S3Object object = amazonS3Client.getObject(bucketName, keyName);
      return object.getObjectContent().readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException("S3 이미지 파일 읽기 실패: " + keyName, e);
    }
  }

  // ========== 정제 데이터 저장/읽기 ==========

  /**
   * 정제된 데이터 저장
   */
  public String saveProcessedData(String category, String fileName, String jsonData) {
    String keyName = String.format("processed-data/%s/%s", category, fileName);
    return saveJsonData("processed-data/" + category, fileName, jsonData);
  }

  /**
   * 정제된 데이터 읽기 (Spark 분석 입력용)
   */
  public String readProcessedDataForAnalysis(String category, String fileName) {
    String keyName = String.format("processed-data/%s/%s", category, fileName);
    return readJsonData(keyName);
  }

  // ========== Spark 분석 결과 저장/읽기 ==========

  /**
   * Spark 분석 결과 저장
   */
  public String saveAnalyticsResult(String analysisType, String fileName, String jsonData) {
    String keyName = String.format("analytics-results/%s/%s", analysisType, fileName);
    return saveJsonData("analytics-results/" + analysisType, fileName, jsonData);
  }

  /**
   * 분석 결과 읽기
   */
  public String readAnalyticsResult(String analysisType, String fileName) {
    String keyName = String.format("analytics-results/%s/%s", analysisType, fileName);
    return readJsonData(keyName);
  }

  // ========== 파일 관리 기능들 ==========

  /**
   * 파일 존재 여부 확인
   */
  public boolean doesFileExist(String keyName) {
    try {
      return amazonS3Client.doesObjectExist(bucketName, keyName);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 폴더 내 파일 목록 조회
   */
  public List<String> listFiles(String prefix) {
    try {
      ListObjectsV2Request req = new ListObjectsV2Request()
          .withBucketName(bucketName)
          .withPrefix(prefix);

      ListObjectsV2Result result = amazonS3Client.listObjectsV2(req);
      return result.getObjectSummaries().stream()
          .map(S3ObjectSummary::getKey)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 목록 조회 실패: " + prefix, e);
    }
  }

  /**
   * 특정 날짜의 크롤링 데이터 모두 읽기 (새로운 구조 반영)
   */
  public List<String> readDailyCrawlingData(String platform, String contentType, String date) {
    String prefix = String.format("raw-data/%s/content/%s/", platform, contentType);
    return listFiles(prefix).stream()
        .filter(key -> key.contains(date))
        .map(this::readJsonData)
        .collect(Collectors.toList());
  }

  /**
   * 플랫폼별 모든 콘텐츠 타입 데이터 읽기
   */
  public List<String> readAllPlatformContent(String platform, String date) {
    String prefix = String.format("raw-data/%s/content/", platform);
    return listFiles(prefix).stream()
        .filter(key -> key.contains(date))
        .filter(key -> key.endsWith(".json"))
        .map(this::readJsonData)
        .collect(Collectors.toList());
  }

  /**
   * 파일 삭제
   */
  public void deleteFile(String keyName) {
    try {
      amazonS3Client.deleteObject(bucketName, keyName);
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 삭제 실패: " + keyName, e);
    }
  }

  /**
   * 파일 메타데이터 조회
   */
  public ObjectMetadata getFileMetadata(String keyName) {
    try {
      return amazonS3Client.getObjectMetadata(bucketName, keyName);
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 메타데이터 조회 실패: " + keyName, e);
    }
  }

  /**
   * 파일 스트림으로 읽기 (큰 파일용)
   */
  public InputStream getFileStream(String keyName) {
    try {
      S3Object object = amazonS3Client.getObject(bucketName, keyName);
      return object.getObjectContent();
    } catch (Exception e) {
      throw new RuntimeException("S3 파일 스트림 읽기 실패: " + keyName, e);
    }
  }

  // ========== 내부 유틸리티 메소드들 ==========

  /**
   * 파일 업로드 공통 메소드
   */
  private String uploadFile(String accessKey, byte[] content, String contentType) {
    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(content.length);
      metadata.setContentType(contentType);

      // 이미지 파일 캐시 설정
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

  /**
   * SNS 크롤링용 이미지 Content-Type 결정 (주요 5가지 지원)
   */
  private String getSnsImageContentType(String imageType) {
    return switch (imageType.toLowerCase()) {
      case "jpg", "jpeg" -> "image/jpeg";    // 90% 커버 (인스타, 유튜브, 틱톡 기본)
      case "webp" -> "image/webp";           // 모바일 최적화 (증가 중)
      case "png" -> "image/png";             // 스크린샷, 그래픽
      case "gif" -> "image/gif";             // 애니메이션 (트위터 등)
      case "heic" -> "image/heic";           // iPhone 사용자 업로드
      default -> "image/jpeg";               // 알 수 없는 확장자는 JPEG로
    };
  }

  /**
   * 파일명에서 확장자 추출
   */
  private String getFileExtension(String fileName) {
    int lastDotIndex = fileName.lastIndexOf('.');
    return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
  }
}