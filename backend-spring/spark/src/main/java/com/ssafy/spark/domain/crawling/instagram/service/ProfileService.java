package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.crawling.instagram.dto.ProfileRawData;
import com.ssafy.spark.domain.crawling.instagram.entity.File;
import com.ssafy.spark.domain.crawling.instagram.entity.Influencer;
import com.ssafy.spark.domain.crawling.instagram.entity.PlatformAccount;
import com.ssafy.spark.domain.crawling.instagram.repository.InfluencerRepository;
import com.ssafy.spark.domain.crawling.instagram.repository.PlatformAccountRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService extends BaseApifyService {

  private final InfluencerRepository influencerRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final S3Service s3Service;
  private final ImageService imageService;
  private final JdbcTemplate jdbcTemplate; // ← 이 줄 추가

  /**
   * 프로필 정보 수집
   */
  public CompletableFuture<String> getProfileOnly(String username,Integer categoryTypeId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("프로필 정보만 수집 시작: {}", username);

        String actorId = "apify~instagram-profile-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsType", "details");
        input.put("usernames", new String[]{username});
        input.put("resultsLimit", 1);

        log.info("프로필 전용 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("프로필 수집 실행 - Run ID: {}", runId);

        return waitAndGetProfileResults(runId, categoryTypeId);

      } catch (Exception e) {
        log.error("프로필 수집 중 오류: ", e);
        return "{\"error\": \"프로필 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  /**
   * 프로필 수집 완료 대기 및 결과 반환
   */
  private String waitAndGetProfileResults(String runId,Integer categoryTypeId) {
    try {
      log.info("프로필 결과 대기 시작 - Run ID: {}", runId);

      // 실행 완료 대기 (최대 5분)
      boolean isCompleted = false;
      int maxAttempts = 30;
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("프로필 실행 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("프로필 수집 실패: {}", status);
          return "{\"error\": \"프로필 수집 실패: " + status + "\"}";
        }

        Thread.sleep(10000); // 10초 대기
      }

      if (isCompleted) {
        String jsonResults = getRunResults(runId);
        log.info("프로필 수집 완료 - 데이터 크기: {} bytes", jsonResults.length());

        // 프로필 전용 파싱 및 출력
        parseAndPrintProfile(jsonResults);

        // DB & S3 저장
        saveProfileToDatabase(jsonResults, categoryTypeId);


        return jsonResults;
      } else {
        String errorMessage = String.format("프로필 수집 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        return "{\"error\": \"" + errorMessage + "\"}";
      }

    } catch (Exception e) {
      log.error("프로필 결과 대기 중 오류: ", e);
      return "{\"error\": \"프로필 결과 대기 중 오류: " + e.getMessage() + "\"}";
    }
  }

  /**
   * 프로필 데이터 파싱 및 출력
   */
  private void parseAndPrintProfile(String jsonResults) {
    try {
      if (jsonResults == null || jsonResults.trim().isEmpty() || "[]".equals(jsonResults.trim())) {
        log.warn("수집된 프로필 정보가 없습니다.");
        return;
      }

      JsonNode results = objectMapper.readTree(jsonResults);

      if (results.size() == 0) {
        log.warn("프로필 결과가 비어있습니다.");
        return;
      }

      // 단일 프로필 정보 처리
      JsonNode profile = results.get(0);
      log.info("=== Instagram 프로필 정보 ===");

      // 실제 필드명에 맞춰서 파싱
      printProfileField(profile, "username", "사용자명");
      printProfileField(profile, "fullName", "전체 이름");
      printProfileField(profile, "biography", "소개");
      printProfileField(profile, "followersCount", "팔로워 수");
      printProfileField(profile, "followsCount", "팔로잉 수");
      printProfileField(profile, "postsCount", "게시물 수");
      printProfileField(profile, "profilePicUrl", "프로필 사진 URL");
      printProfileField(profile, "verified", "인증 계정");
      printProfileField(profile, "private", "비공개 계정");
      printProfileField(profile, "url", "외부 계정 url");
      printProfileField(profile, "businessCategoryName", "비즈니스 카테고리");

      if (profile.has("latestPosts")) {
        log.info("게시물 개수: {}", profile.path("latestPosts").size());
      }

    } catch (Exception e) {
      log.error("프로필 파싱 중 오류: ", e);
    }
  }

  /**
   * 프로필 필드 출력 헬퍼 메서드
   */
  private void printProfileField(JsonNode profile, String fieldName, String displayName) {
    if (profile.has(fieldName) && !profile.path(fieldName).isNull()) {
      JsonNode field = profile.path(fieldName);
      if (field.isTextual()) {
        String text = field.asText();
        if (!text.isEmpty()) {
          log.info("{}: {}", displayName, text.length() > 200 ? text.substring(0, 200) + "..." : text);
        }
      } else {
        log.info("{}: {}", displayName, field.asText());
      }
    }
  }

  private Influencer createInfluencerFromProfile(JsonNode profile, File profileImageFile) {
    String username = profile.path("username").asText();
    String fullName = profile.path("fullName").asText();
    String biography = profile.path("biography").asText();

    return Influencer.builder()
        .providerTypeId("GOOGLE")
        .providerMemberId(username!=null?username:LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
        .nickname(fullName.isEmpty() ? username : fullName)
        .gender(true)
        .birthday(LocalDate.now().minusYears(25))
        .email(username + "@instagram.com")
        .bio(biography.length() > 401 ? biography.substring(0, 401) : biography)
        .influencerProfileImageId(profileImageFile != null ? profileImageFile.getId() : null)
        .isDeleted(false)
        .build();
  }

  private PlatformAccount createPlatformAccountFromProfile(JsonNode profile, Integer influencerId, Integer categoryTypeId, File profileImageFile) {
    String username = profile.path("username").asText();
    String fullName = profile.path("fullName").asText();
    String url = profile.path("url").asText();


    return PlatformAccount.builder()
        .influencerId(influencerId)
        .platformTypeId("INSTAGRAM")
        .externalAccountId(profile.path("id").asText())
        .accountNickname(username)
        .accountUrl(url)
        .accountProfileImageId(profileImageFile != null ? profileImageFile.getId() : null)
        .categoryTypeId(categoryTypeId)
        .isDeleted(false)
        .deletedAt(LocalDateTime.now())
        .build();
  }

  private void saveProfileToDatabase(String jsonResults, Integer categoryTypeId) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      JsonNode profile = results.get(0);

      String profilePicUrl = profile.path("profilePicUrl").asText();
      String username = profile.path("username").asText();

      File profileImageFile = null;
      if (!profilePicUrl.isEmpty()) {
        profileImageFile = imageService.downloadAndSaveProfileImage(profilePicUrl, username);
      }

      Influencer influencer = createInfluencerFromProfile(profile, profileImageFile);
      Influencer savedInfluencer = influencerRepository.save(influencer);

      PlatformAccount platformAccount = createPlatformAccountFromProfile(profile, savedInfluencer.getId(), categoryTypeId, profileImageFile);
      platformAccountRepository.save(platformAccount);

      saveProfileToS3(profile, categoryTypeId);

      log.info("DB 및 S3 저장 완료 - Influencer ID: {}, Category: {}", savedInfluencer.getId(), categoryTypeId);

    } catch (Exception e) {
      log.error("DB/S3 저장 중 오류: ", e);
    }
  }

  private void saveProfileToS3(JsonNode profile, Integer categoryTypeId) {
    try {
      String externalAccountId = profile.path("id").asText();        // "58740544295"
      String accountNickname = profile.path("username").asText();

      String categoryName = getCategoryNameById(categoryTypeId);
      String accountUrl = profile.path("url").asText();
      Integer followersCount = profile.path("followersCount").asInt();
      Integer postsCount = profile.path("postsCount").asInt();

      ProfileRawData rawData = ProfileRawData.builder()
          .externalAccountId(externalAccountId)
          .accountNickname(accountNickname.isEmpty() ? externalAccountId : accountNickname)
          .categoryName(categoryName)
          .accountUrl(accountUrl)
          .followersCount(followersCount)
          .postsCount(postsCount)
          .crawledAt(Instant.now().toString())
          .build();

      // Pretty-print JSON으로 저장
      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(rawData);

      s3Service.uploadProfileData(jsonData, profile.path("username").asText());

    } catch (Exception e) {
      log.error("S3 저장 중 오류: ", e);
    }
  }

  //======================================================================================
  /**
   * 기존 인플루언서에 새 플랫폼 계정 연결
   */
  public CompletableFuture<String> linkPlatformAccountToExistingInfluencer(String username, Integer existingInfluencerId, Integer categoryTypeId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("기존 인플루언서({})에 플랫폼 계정 연결 시작: {}", existingInfluencerId, username);

        String actorId = "apify~instagram-profile-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsType", "details");
        input.put("usernames", new String[]{username});
        input.put("resultsLimit", 1);

        String runId = runActor(actorId, input);
        log.info("프로필 수집 실행 - Run ID: {}", runId);

        return waitAndLinkToExistingInfluencer(runId, existingInfluencerId, categoryTypeId);

      } catch (Exception e) {
        log.error("기존 인플루언서 연결 중 오류: ", e);
        return "{\"error\": \"연결 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  private String waitAndLinkToExistingInfluencer(String runId, Integer existingInfluencerId, Integer categoryTypeId) {
    try {
      // 기존 waitAndGetProfileResults와 동일한 대기 로직
      boolean isCompleted = false;
      int maxAttempts = 30;
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("프로필 실행 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("프로필 수집 실패: {}", status);
          return "{\"error\": \"프로필 수집 실패: " + status + "\"}";
        }

        Thread.sleep(10000);
      }

      if (isCompleted) {
        String jsonResults = getRunResults(runId);
        linkProfileToExistingInfluencer(jsonResults, existingInfluencerId, categoryTypeId);
        return jsonResults;
      } else {
        String errorMessage = String.format("프로필 수집 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        return "{\"error\": \"" + errorMessage + "\"}";
      }

    } catch (Exception e) {
      log.error("기존 인플루언서 연결 대기 중 오류: ", e);
      return "{\"error\": \"연결 대기 중 오류: " + e.getMessage() + "\"}";
    }
  }

  private void linkProfileToExistingInfluencer(String jsonResults, Integer existingInfluencerId, Integer categoryTypeId) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      JsonNode profile = results.get(0);

      Influencer existingInfluencer = influencerRepository.findById(existingInfluencerId)
          .orElseThrow(() -> new RuntimeException("존재하지 않는 인플루언서: " + existingInfluencerId));
      // 프로필 이미지 다운로드 (누락된 부분)
      String profilePicUrl = profile.path("profilePicUrl").asText();
      String username = profile.path("username").asText();

      File profileImageFile = null;
      if (!profilePicUrl.isEmpty()) {
        profileImageFile = imageService.downloadAndSaveProfileImage(profilePicUrl, username);
      }

      PlatformAccount platformAccount = createPlatformAccountFromProfile(profile, existingInfluencerId, categoryTypeId, profileImageFile);
      platformAccountRepository.save(platformAccount);

      saveProfileToS3(profile, categoryTypeId);

      log.info("기존 인플루언서에 플랫폼 계정 연결 완료 - Influencer ID: {}, Category: {}",
          existingInfluencerId, categoryTypeId);

    } catch (Exception e) {
      log.error("기존 인플루언서 연결 중 오류: ", e);
    }
  }
  /**
   * 카테고리 ID로 카테고리명 조회
   */
  private String getCategoryNameById(Integer categoryTypeId) {
    try {
      String sql = "SELECT category_name FROM category_type WHERE category_type_id = ?";
      return jdbcTemplate.queryForObject(sql, String.class, categoryTypeId);
    } catch (Exception e) {
      log.warn("카테고리 조회 실패: categoryTypeId={}", categoryTypeId);
      return "기타"; // 기본값
    }
  }


}