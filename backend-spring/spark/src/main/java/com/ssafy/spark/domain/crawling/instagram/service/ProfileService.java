package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.crawling.connect.request.CrawlingRequest;
import com.ssafy.spark.domain.crawling.instagram.dto.ProfileRawData;
import java.time.Instant;
import java.time.LocalDateTime;
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

  private final PlatformAccountRepository platformAccountRepository;
  private final S3Service s3Service;
  private final ImageService imageService;
  private final JdbcTemplate jdbcTemplate;

  /**
   * 프로필 정보 수집
   */
  public CompletableFuture<String> getProfileOnly( CrawlingRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {

        String actorId = "apify~instagram-profile-scraper";

        Map<String, Object> input = new HashMap<>();
        input.put("resultsType", "details");
        input.put("usernames", new String[]{request.getAccountNickname()});
        input.put("resultsLimit", 1);

        log.info("프로필 전용 입력: {}", objectMapper.writeValueAsString(input));

        String runId = runActor(actorId, input);
        log.info("프로필 수집 실행 - Run ID: {}", runId);

        PlatformAccount platformAccount = platformAccountRepository.findWithAllRelationsById(request.getPlatformAccountId()).get();
        return waitAndUpdateProfile(runId, platformAccount);

      } catch (Exception e) {
        log.error("프로필 수집 중 오류: ", e);
        return "{\"error\": \"프로필 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  /**
   * 프로필 수집 완료 대기 및 결과 반환
   */

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


  private void saveProfileToS3(JsonNode profile, PlatformAccount platformAccount) {
    try {
      String externalAccountId = profile.path("id").asText();
      String accountNickname = profile.path("username").asText();

      String categoryName =platformAccount.getCategoryType().getCategoryName();
      String accountUrl = profile.path("url").asText();
      Integer followersCount = profile.path("followersCount").asInt();
      Integer postsCount = profile.path("postsCount").asInt();
      String profilePicUrl = profile.path("profilePicUrl").asText();

      //이미지 저장
      if (platformAccount.getAccountProfileImageId()==null) {
        File profileImageFile = imageService.downloadAndSaveProfileImage(profilePicUrl, accountNickname);
        platformAccount.setAccountProfileImageId(profileImageFile.getId());
        platformAccountRepository.save(platformAccount);
      }

      // .json 저장
      ProfileRawData rawData = ProfileRawData.builder()
          .externalAccountId(externalAccountId)
          .accountNickname(accountNickname.isEmpty() ? externalAccountId : accountNickname)
          .categoryName(categoryName)
          .accountUrl(accountUrl)
          .followersCount(followersCount)
          .postsCount(postsCount)
          .crawledAt(LocalDateTime.now().toString())
          .build();

      // Pretty-print JSON으로 저장
      String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(rawData);

      s3Service.uploadProfileData(jsonData, profile.path("username").asText());

    } catch (Exception e) {
      log.error("S3 저장 중 오류: ", e);
    }
  }


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
  private String waitAndUpdateProfile(String runId, PlatformAccount platformAccount) {
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
        return jsonResults;
      } else {
        throw new RuntimeException("프로필 업데이트 시간 초과");
      }
    } catch (Exception e) {
      log.error("프로필 업데이트 중 오류", e);
      throw new RuntimeException("프로필 업데이트 실패", e);
    }
  }

  /**
   * 프로필 정보를 S3 업데이트 저장
   */
  private void updateProfileToS3(String jsonResults, PlatformAccount platformAccount) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      if (results.size() == 0) {
        log.warn("업데이트할 프로필 결과가 비어있습니다.");
        return;
      }

      JsonNode profile = results.get(0);

      // S3에 업데이트된 프로필 정보 저장
      saveProfileToS3(profile, platformAccount);
      log.info("프로필 정보 S3 업데이트 완료 - 계정: {}",platformAccount.getAccountNickname());

    } catch (Exception e) {
      log.error("프로필 S3 업데이트 중 오류 - 계정: {}", platformAccount.getAccountNickname(), e);
      throw new RuntimeException("프로필 S3 업데이트 실패", e);
    }
  }
}