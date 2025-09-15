package com.ssafy.leaper.domain.pipeline.controller;

import com.ssafy.leaper.domain.pipeline.service.S3DataService;
import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.impl.ApiSuccessResponse;
import com.ssafy.leaper.global.common.response.impl.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/s3")
public class S3FolderTestController {

  @Autowired
  private S3DataService s3DataService;

  /**
   * 1️⃣ S3 연결 테스트 - 가장 먼저 실행해보세요!
   */
  @PostMapping("/connection-test")
  public ApiResponse<String> testConnection() {
    try {
      String testData = "S3 연결 테스트: " + LocalDateTime.now();
      String url = s3DataService.saveJsonData("test", "connection_test.json", testData);
      return ApiSuccessResponse.success(url);
    } catch (Exception e) {
      return ApiErrorResponse.error("S3_CONNECTION_FAILED", e.getMessage());
    }
  }

  /**
   * 2️⃣ 새로운 폴더 구조 생성 테스트 - 플랫폼별 콘텐츠 타입 폴더
   */
  @PostMapping("/create-new-structure")
  public ApiResponse<Map<String, String>> createNewStructure() {
    Map<String, String> results = new HashMap<>();

    try {
      byte[] testImageData = "테스트 썸네일 이미지 데이터".getBytes();

      // ========== Instagram 구조 테스트 ==========

      // Instagram Account 저장
      String instagramAccountJson = """
        {
          "platformAccountId": "account_456",
          "username": "test_instagram_user",
          "followerCount": 1500,
          "platformType": "instagram",
          "createdAt": "%s"
        }
        """.formatted(LocalDateTime.now());

      String instagramAccountUrl = s3DataService.saveInstagramAccount("account_456", instagramAccountJson, testImageData, "jpg");
      results.put("instagram_account", instagramAccountUrl);

      // Instagram POST 콘텐츠
      String instagramPostJson = """
        {
          "contentId": "inst_post_123",
          "platformAccountId": "account_456",
          "platformType": "instagram",
          "contentType": "POST",
          "title": "테스트 인스타 포스트",
          "description": "인스타그램 포스트 테스트",
          "publishedAt": "%s",
          "totalLikes": 100,
          "totalComments": 20
        }
        """.formatted(LocalDateTime.now());

      String instagramPostUrl = s3DataService.saveInstagramContent("POST", "inst_post_123", instagramPostJson, testImageData, "jpg");
      results.put("instagram_post", instagramPostUrl);

      // Instagram VIDEO_SHORT (릴스) 콘텐츠
      String instagramReelsJson = """
        {
          "contentId": "inst_reels_124",
          "platformAccountId": "account_456",
          "platformType": "instagram",
          "contentType": "VIDEO_SHORT",
          "title": "테스트 인스타 릴스",
          "durationSeconds": 30,
          "publishedAt": "%s",
          "totalViews": 2500
        }
        """.formatted(LocalDateTime.now());

      String instagramReelsUrl = s3DataService.saveInstagramContent("VIDEO_SHORT", "inst_reels_124", instagramReelsJson, testImageData, "jpg");
      results.put("instagram_reels", instagramReelsUrl);

      // Instagram VIDEO_LONG (IGTV) 콘텐츠
      String instagramIgtvJson = """
        {
          "contentId": "inst_igtv_125",
          "platformAccountId": "account_456",
          "platformType": "instagram",
          "contentType": "VIDEO_LONG",
          "title": "테스트 IGTV",
          "durationSeconds": 600,
          "publishedAt": "%s",
          "totalViews": 800
        }
        """.formatted(LocalDateTime.now());

      String instagramIgtvUrl = s3DataService.saveInstagramContent("VIDEO_LONG", "inst_igtv_125", instagramIgtvJson, testImageData, "jpg");
      results.put("instagram_igtv", instagramIgtvUrl);

      // ========== YouTube 구조 테스트 ==========

      // YouTube Channel 저장
      String youtubeChannelJson = """
        {
          "platformAccountId": "channel_789",
          "channelName": "테스트 유튜브 채널",
          "subscriberCount": 50000,
          "platformType": "youtube",
          "createdAt": "%s"
        }
        """.formatted(LocalDateTime.now());

      String youtubeChannelUrl = s3DataService.saveYoutubeAccount("channel_789", youtubeChannelJson, testImageData, "jpg");
      results.put("youtube_channel", youtubeChannelUrl);

      // YouTube POST (커뮤니티) 콘텐츠
      String youtubePostJson = """
        {
          "contentId": "yt_post_200",
          "platformAccountId": "channel_789",
          "platformType": "youtube",
          "contentType": "POST",
          "title": "유튜브 커뮤니티 포스트",
          "description": "커뮤니티 탭 테스트",
          "publishedAt": "%s",
          "totalLikes": 150
        }
        """.formatted(LocalDateTime.now());

      String youtubePostUrl = s3DataService.saveYoutubeContent("POST", "yt_post_200", youtubePostJson, testImageData, "jpg");
      results.put("youtube_post", youtubePostUrl);

      // YouTube VIDEO_SHORT (쇼츠) 콘텐츠
      String youtubeShortsJson = """
        {
          "contentId": "yt_shorts_201",
          "platformAccountId": "channel_789",
          "platformType": "youtube",
          "contentType": "VIDEO_SHORT",
          "title": "유튜브 쇼츠 테스트",
          "durationSeconds": 45,
          "publishedAt": "%s",
          "totalViews": 5000
        }
        """.formatted(LocalDateTime.now());

      String youtubeShortsUrl = s3DataService.saveYoutubeContent("VIDEO_SHORT", "yt_shorts_201", youtubeShortsJson, testImageData, "jpg");
      results.put("youtube_shorts", youtubeShortsUrl);

      // YouTube VIDEO_LONG (일반 영상) 콘텐츠
      String youtubeLongJson = """
        {
          "contentId": "yt_long_202",
          "platformAccountId": "channel_789",
          "platformType": "youtube",
          "contentType": "VIDEO_LONG",
          "title": "유튜브 일반 영상",
          "durationSeconds": 1200,
          "publishedAt": "%s",
          "totalViews": 12000
        }
        """.formatted(LocalDateTime.now());

      String youtubeLongUrl = s3DataService.saveYoutubeContent("VIDEO_LONG", "yt_long_202", youtubeLongJson, testImageData, "jpg");
      results.put("youtube_long_video", youtubeLongUrl);

      // ========== Naver Blog 구조 테스트 ==========

      // Naver Blog Account 저장
      String naverBlogAccountJson = """
        {
          "platformAccountId": "blogger_999",
          "blogName": "테스트 네이버 블로그",
          "blogUrl": "https://blog.naver.com/testblog",
          "platformType": "naver-blog",
          "createdAt": "%s"
        }
        """.formatted(LocalDateTime.now());

      String naverBlogAccountUrl = s3DataService.saveNaverBlogAccount("blogger_999", naverBlogAccountJson, testImageData, "jpg");
      results.put("naver_blog_account", naverBlogAccountUrl);

      // Naver Blog POST 콘텐츠
      String naverBlogPostJson = """
        {
          "contentId": "naver_post_300",
          "platformAccountId": "blogger_999",
          "platformType": "naver-blog",
          "contentType": "POST",
          "title": "네이버 블로그 포스트",
          "description": "블로그 포스트 테스트",
          "publishedAt": "%s",
          "totalViews": 800
        }
        """.formatted(LocalDateTime.now());

      String naverBlogPostUrl = s3DataService.saveNaverBlogContent("naver_post_300", naverBlogPostJson, testImageData, "jpg");
      results.put("naver_blog_post", naverBlogPostUrl);

      // ========== 정제 데이터 및 분석 결과 ==========

      // processed-data/daily/ 폴더 생성
      String dailyUrl = s3DataService.saveProcessedData("daily", "test_daily.json", """
        {
          "type": "daily_processed",
          "message": "일별 정제 데이터 폴더 생성 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("daily_processed", dailyUrl);

      // processed-data/monthly/ 폴더 생성
      String monthlyUrl = s3DataService.saveProcessedData("monthly", "test_monthly.json", """
        {
          "type": "monthly_processed",
          "message": "월별 정제 데이터 폴더 생성 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("monthly_processed", monthlyUrl);

      // analytics-results/trends/ 폴더 생성
      String trendsUrl = s3DataService.saveAnalyticsResult("trends", "test_trends.json", """
        {
          "type": "trend_analysis",
          "message": "트렌드 분석 결과 폴더 생성 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("trends_analysis", trendsUrl);

      // analytics-results/insights/ 폴더 생성
      String insightsUrl = s3DataService.saveAnalyticsResult("insights", "test_insights.json", """
        {
          "type": "insight_analysis",
          "message": "인사이트 분석 결과 폴더 생성 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("insights_analysis", insightsUrl);

      return ApiSuccessResponse.success(results);

    } catch (Exception e) {
      Map<String, String> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("NEW_STRUCTURE_CREATION_FAILED", errorResult);
    }
  }

  /**
   * 3️⃣ 개별 파일 저장 테스트 (JSON만, 썸네일만)
   */
  @PostMapping("/test-individual-saves")
  public ApiResponse<Map<String, String>> testIndividualSaves() {
    Map<String, String> results = new HashMap<>();

    try {
      // JSON만 저장 테스트
      String jsonOnlyResult = s3DataService.saveContentJson("instagram", "POST", "json_only_test", """
        {
          "contentId": "json_only_test",
          "message": "JSON만 저장하는 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("json_only", jsonOnlyResult);

      // 썸네일만 저장 테스트
      byte[] thumbnailData = "썸네일 전용 이미지 데이터".getBytes();
      String thumbnailOnlyResult = s3DataService.saveThumbnailImage("youtube", "VIDEO_SHORT", "thumb_only_test", thumbnailData, "png");
      results.put("thumbnail_only", thumbnailOnlyResult);

      return ApiSuccessResponse.success(results);

    } catch (Exception e) {
      Map<String, String> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("INDIVIDUAL_SAVE_FAILED", errorResult);
    }
  }

  /**
   * 4️⃣ 새로운 폴더 구조 확인
   */
  @GetMapping("/check-new-structure")
  public ApiResponse<Map<String, Object>> checkNewStructure() {
    try {
      Map<String, Object> folderStatus = new HashMap<>();

      // Instagram 구조 확인
      folderStatus.put("instagram_platform_account", s3DataService.listFiles("raw-data/instagram/platform-account/").size());
      folderStatus.put("instagram_content_post", s3DataService.listFiles("raw-data/instagram/content/post/").size());
      folderStatus.put("instagram_content_video_short", s3DataService.listFiles("raw-data/instagram/content/video-short/").size());
      folderStatus.put("instagram_content_video_long", s3DataService.listFiles("raw-data/instagram/content/video-long/").size());

      // YouTube 구조 확인
      folderStatus.put("youtube_platform_account", s3DataService.listFiles("raw-data/youtube/platform-account/").size());
      folderStatus.put("youtube_content_post", s3DataService.listFiles("raw-data/youtube/content/post/").size());
      folderStatus.put("youtube_content_video_short", s3DataService.listFiles("raw-data/youtube/content/video-short/").size());
      folderStatus.put("youtube_content_video_long", s3DataService.listFiles("raw-data/youtube/content/video-long/").size());

      // Naver Blog 구조 확인
      folderStatus.put("naver_blog_platform_account", s3DataService.listFiles("raw-data/naver-blog/platform-account/").size());
      folderStatus.put("naver_blog_content_post", s3DataService.listFiles("raw-data/naver-blog/content/post/").size());

      // 정제 데이터 및 분석 결과 확인
      folderStatus.put("processed_data_daily", s3DataService.listFiles("processed-data/daily/").size());
      folderStatus.put("processed_data_monthly", s3DataService.listFiles("processed-data/monthly/").size());
      folderStatus.put("analytics_results_trends", s3DataService.listFiles("analytics-results/trends/").size());
      folderStatus.put("analytics_results_insights", s3DataService.listFiles("analytics-results/insights/").size());

      // 전체 파일 목록
      Map<String, List<String>> allFiles = new HashMap<>();
      allFiles.put("all_raw_data_files", s3DataService.listFiles("raw-data/"));
      allFiles.put("all_processed_files", s3DataService.listFiles("processed-data/"));
      allFiles.put("all_analytics_files", s3DataService.listFiles("analytics-results/"));

      Map<String, Object> result = new HashMap<>();
      result.put("folder_file_counts", folderStatus);
      result.put("file_lists", allFiles);
      result.put("message", "새로운 S3 폴더 구조 확인 완료");

      return ApiSuccessResponse.success(result);

    } catch (Exception e) {
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("NEW_STRUCTURE_CHECK_FAILED", errorResult);
    }
  }

  /**
   * 5️⃣ 특정 파일 읽기 테스트 (accessKey 사용)
   */
  @GetMapping("/read-file")
  public ApiResponse<Map<String, Object>> readFile(@RequestParam String accessKey) {
    try {
      if (!s3DataService.doesFileExist(accessKey)) {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", "파일이 존재하지 않습니다");
        errorResult.put("accessKey", accessKey);
        return ApiErrorResponse.error("FILE_NOT_FOUND", errorResult);
      }

      String content = s3DataService.readJsonData(accessKey);

      Map<String, Object> result = new HashMap<>();
      result.put("accessKey", accessKey);
      result.put("content", content);
      result.put("message", "파일 읽기 성공");

      return ApiSuccessResponse.success(result);

    } catch (Exception e) {
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("FILE_READ_FAILED", errorResult);
    }
  }

  /**
   * 6️⃣ 플랫폼별 콘텐츠 조회 테스트
   */
  @GetMapping("/read-platform-content")
  public ApiResponse<Map<String, Object>> readPlatformContent(
      @RequestParam String platform,
      @RequestParam String date) {
    try {
      List<String> allContent = s3DataService.readAllPlatformContent(platform, date);

      Map<String, Object> result = new HashMap<>();
      result.put("platform", platform);
      result.put("date", date);
      result.put("contentCount", allContent.size());
      result.put("contents", allContent);

      return ApiSuccessResponse.success(result);

    } catch (Exception e) {
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("PLATFORM_CONTENT_READ_FAILED", errorResult);
    }
  }

  /**
   * 7️⃣ 콘텐츠 타입별 조회 테스트
   */
  @GetMapping("/read-content-by-type")
  public ApiResponse<Map<String, Object>> readContentByType(
      @RequestParam String platform,
      @RequestParam String contentType,
      @RequestParam String date) {
    try {
      List<String> contentList = s3DataService.readDailyCrawlingData(platform, contentType, date);

      Map<String, Object> result = new HashMap<>();
      result.put("platform", platform);
      result.put("contentType", contentType);
      result.put("date", date);
      result.put("contentCount", contentList.size());
      result.put("contents", contentList);

      return ApiSuccessResponse.success(result);

    } catch (Exception e) {
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("CONTENT_TYPE_READ_FAILED", errorResult);
    }
  }

  /**
   * 8️⃣ 레거시 메소드 테스트 (Deprecated 메소드들)
   */
  @PostMapping("/test-legacy-methods")
  public ApiResponse<Map<String, String>> testLegacyMethods() {
    Map<String, String> results = new HashMap<>();

    try {
      // 레거시 인스타그램 포스트 저장
      String legacyInstagramUrl = s3DataService.saveInstagramPost("""
        {
          "message": "레거시 인스타그램 포스트 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("legacy_instagram", legacyInstagramUrl);

      // 레거시 유튜브 비디오 저장
      String legacyYoutubeUrl = s3DataService.saveYoutubeVideoInfo("""
        {
          "message": "레거시 유튜브 비디오 테스트",
          "timestamp": "%s"
        }
        """.formatted(LocalDateTime.now()));
      results.put("legacy_youtube", legacyYoutubeUrl);

      return ApiSuccessResponse.success(results);

    } catch (Exception e) {
      Map<String, String> errorResult = new HashMap<>();
      errorResult.put("error", e.getMessage());
      return ApiErrorResponse.error("LEGACY_METHOD_TEST_FAILED", errorResult);
    }
  }

  /**
   * 9️⃣ 테스트 파일들 정리 (선택사항)
   */
  @DeleteMapping("/cleanup-test-files")
  public ApiResponse<String> cleanupTestFiles() {
    try {
      // test/ 폴더의 모든 파일 삭제
      List<String> testFiles = s3DataService.listFiles("test/");
      for (String file : testFiles) {
        s3DataService.deleteFile(file);
      }

      return ApiSuccessResponse.success(String.format("%d개 파일 삭제됨", testFiles.size()));

    } catch (Exception e) {
      return ApiErrorResponse.error("CLEANUP_FAILED", e.getMessage());
    }
  }
}