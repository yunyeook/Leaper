package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import com.ssafy.spark.domain.business.influencer.repository.InfluencerRepository;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.business.type.entity.CategoryType;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.domain.business.type.entity.ProviderType;
import com.ssafy.spark.domain.business.type.repository.CategoryTypeRepository;
import com.ssafy.spark.domain.business.type.repository.PlatformTypeRepository;
import com.ssafy.spark.domain.business.type.repository.ProviderTypeRepository;
import com.ssafy.spark.domain.crawling.instagram.dto.ProfileRawData;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
  private final ProviderTypeRepository providerTypeRepository;
  private final PlatformTypeRepository platformTypeRepository;
  private final CategoryTypeRepository categoryTypeRepository;
  private final S3Service s3Service;
  private final ImageService imageService;
  private final JdbcTemplate jdbcTemplate;

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
      int maxAttempts = 10;
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
    ProviderType googleType = providerTypeRepository.findById("GOOGLE")
        .orElseThrow(() -> new IllegalArgumentException("ProviderType GOOGLE not found"));
    return Influencer.builder()
        .providerType(googleType)
        .providerMemberId(username!=null?username:LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
        .nickname(fullName.isEmpty() ? username : fullName)
        .gender(true)
        .birthday(LocalDate.now().minusYears(25))
        .email(username + "@instagram.com")
        .bio(biography.length() > 401 ? biography.substring(0, 401) : biography)
        .profileImageId(profileImageFile != null ? profileImageFile.getId() : null)
        .isDeleted(false)
        .build();
  }

  private PlatformAccount createPlatformAccountFromProfile(
      JsonNode profile,
      Influencer influencer,            // ✅ 엔티티 직접 받기
      PlatformType platformType,        // ✅ 엔티티 직접 받기
      CategoryType categoryType,        // ✅ 엔티티 직접 받기
      File profileImageFile
  ) {
    String username = profile.path("username").asText();
    String fullName = profile.path("fullName").asText();
    String url = profile.path("url").asText();

    return PlatformAccount.builder()
        .influencer(influencer)               // ✅ Influencer 엔티티
        .platformType(platformType)           // ✅ PlatformType 엔티티
        .externalAccountId(profile.path("id").asText())
        .accountNickname(username)
        .accountUrl(url)
        .accountProfileImageId(profileImageFile != null ? profileImageFile.getId() : null)
        .categoryType(categoryType)           // ✅ CategoryType 엔티티
        .isDeleted(false)
        .deletedAt(null)
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

// 1. 필요한 엔티티들 조회
      PlatformType platformType = platformTypeRepository.findById("INSTAGRAM")
          .orElseThrow(() -> new IllegalArgumentException("INSTAGRAM 플랫폼 타입을 찾을 수 없습니다"));

      CategoryType categoryType = categoryTypeRepository.findById((short)categoryTypeId.intValue())
          .orElseThrow(() -> new IllegalArgumentException("카테고리 타입을 찾을 수 없습니다: " + categoryTypeId));

// 2. 엔티티 객체들을 전달
      PlatformAccount platformAccount = createPlatformAccountFromProfile(profile, savedInfluencer, platformType, categoryType, profileImageFile);

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
          .crawledAt(Instant.now().toString()) //TODO : 날짜  utc인데.. 아시아서울하려면 localdate?
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
   * 기존 인플루언서에 인스타 플랫폼 계정 연결
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

      PlatformType platformType = platformTypeRepository.findById("INSTAGRAM")
          .orElseThrow(() -> new IllegalArgumentException("INSTAGRAM 플랫폼 타입을 찾을 수 없습니다"));

      CategoryType categoryType = categoryTypeRepository.findById((short)categoryTypeId.intValue())
          .orElseThrow(() -> new IllegalArgumentException("카테고리 타입을 찾을 수 없습니다: " + categoryTypeId));

      // 2. 엔티티 객체들을 전달
      PlatformAccount platformAccount = createPlatformAccountFromProfile(profile, existingInfluencer, platformType, categoryType, profileImageFile);

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

//============================================================================================================

  /**
   * DB에 저장된 모든 Instagram 계정의 프로필 정보 일괄 업데이트 (S3에만 저장)
   */
  public void updateAllRegisteredUsersProfiles() {
    try {
      log.info("DB에서 Instagram 계정 목록 조회 시작");

      // DB에서 INSTAGRAM 플랫폼 계정들 조회
      List<PlatformAccount> instagramAccounts = platformAccountRepository
          .findByPlatformTypeIdAndIsDeleted("INSTAGRAM", false);

      log.info("업데이트할 Instagram 계정 수: {}", instagramAccounts.size());

      if (instagramAccounts.isEmpty()) {
        log.info("업데이트할 Instagram 계정이 없습니다.");
        return;
      }

      // 각 계정별로 프로필 업데이트 실행
      for (PlatformAccount account : instagramAccounts) {
        try {
          log.info("프로필 업데이트 시작 - 계정: {}, ID: {}",
              account.getAccountNickname(), account.getId());

          updateSingleUserProfile(account);

          // API 호출 제한을 위한 딜레이 (5초)
          Thread.sleep(5000);

          log.info("프로필 업데이트 완료 - 계정: {}", account.getAccountNickname());

        } catch (Exception e) {
          log.error("프로필 업데이트 실패 - 계정: {}, 오류: {}",
              account.getAccountNickname(), e.getMessage(), e);
          // 개별 계정 실패시에도 다음 계정 계속 처리
          continue;
        }
      }

      log.info("전체 Instagram 계정 프로필 업데이트 작업 완료");

    } catch (Exception e) {
      log.error("프로필 일괄 업데이트 중 오류: ", e);
      throw new RuntimeException("프로필 일괄 업데이트 실패", e);
    }
  }

  /**
   * 단일 사용자의 프로필 정보 업데이트
   */
  private void updateSingleUserProfile(PlatformAccount platformAccount) {
    try {
      String username = platformAccount.getAccountNickname();
      log.info("단일 프로필 업데이트 시작: {}", username);

      String actorId = "apify~instagram-profile-scraper";

      Map<String, Object> input = new HashMap<>();
      input.put("resultsType", "details");
      input.put("usernames", new String[]{username});
      input.put("resultsLimit", 1);

      log.info("프로필 업데이트 입력: {}", objectMapper.writeValueAsString(input));

      String runId = runActor(actorId, input);
      log.info("프로필 업데이트 실행 - Run ID: {}", runId);

      // 결과 대기 및 업데이트 처리
      waitAndUpdateProfile(runId, platformAccount);

    } catch (Exception e) {
      log.error("단일 프로필 업데이트 중 오류 - 계정: {}", platformAccount.getAccountNickname(), e);
      throw new RuntimeException("프로필 업데이트 실패: " + platformAccount.getAccountNickname(), e);
    }
  }

  /**
   * 프로필 업데이트 완료 대기 및 S3 저장
   */
  private void waitAndUpdateProfile(String runId, PlatformAccount platformAccount) {
    try {
      log.info("프로필 업데이트 결과 대기 시작 - Run ID: {}", runId);

      // 실행 완료 대기 (최대 5분)
      boolean isCompleted = false;
      int maxAttempts = 30;
      String finalStatus = "";

      for (int i = 0; i < maxAttempts; i++) {
        String status = checkRunStatus(runId);
        finalStatus = status;
        log.info("프로필 업데이트 상태 확인 ({}/{}): {}", i + 1, maxAttempts, status);

        if ("SUCCEEDED".equals(status)) {
          isCompleted = true;
          break;
        } else if ("FAILED".equals(status) || "ABORTED".equals(status)) {
          log.error("프로필 업데이트 실패: {}", status);
          throw new RuntimeException("프로필 업데이트 실패: " + status);
        }

        Thread.sleep(10000); // 10초 대기
      }

      if (isCompleted) {
        String jsonResults = getRunResults(runId);
        log.info("프로필 업데이트 완료 - 데이터 크기: {} bytes", jsonResults.length());

        // 프로필 정보 파싱 및 출력
        parseAndPrintProfile(jsonResults);

        // S3에만 업데이트된 프로필 정보 저장
        updateProfileToS3(jsonResults, platformAccount);

      } else {
        String errorMessage = String.format("프로필 업데이트 시간 초과 - Run ID: %s, 마지막 상태: %s", runId, finalStatus);
        log.warn(errorMessage);
        throw new RuntimeException(errorMessage);
      }

    } catch (Exception e) {
      log.error("프로필 업데이트 대기 중 오류: ", e);
      throw new RuntimeException("프로필 업데이트 대기 중 오류", e);
    }
  }

  /**
   * 프로필 정보를 S3에만 업데이트 저장
   */
  private void updateProfileToS3(String jsonResults, PlatformAccount platformAccount) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      if (results.size() == 0) {
        log.warn("업데이트할 프로필 결과가 비어있습니다.");
        return;
      }

      JsonNode profile = results.get(0);
      String username = platformAccount.getAccountNickname();

      // S3에 업데이트된 프로필 정보 저장
      saveProfileToS3(profile, platformAccount.getCategoryType().getId().intValue());
      log.info("프로필 정보 S3 업데이트 완료 - 계정: {}", username);

    } catch (Exception e) {
      log.error("프로필 S3 업데이트 중 오류 - 계정: {}", platformAccount.getAccountNickname(), e);
      throw new RuntimeException("프로필 S3 업데이트 실패", e);
    }
  }
}