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

  /**
   * 프로필 정보 수집
   */
  public CompletableFuture<String> getProfileOnly(String username) {
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

        return waitAndGetProfileResults(runId);

      } catch (Exception e) {
        log.error("프로필 수집 중 오류: ", e);
        return "{\"error\": \"프로필 수집 실패: " + e.getMessage() + "\"}";
      }
    });
  }

  /**
   * 프로필 수집 완료 대기 및 결과 반환
   */
  private String waitAndGetProfileResults(String runId) {
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
        saveProfileToDatabase(jsonResults);



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

  private Influencer createInfluencerFromProfile(JsonNode profile) {
    String username = profile.path("username").asText();
    String fullName = profile.path("fullName").asText();
    String biography = profile.path("biography").asText();

    return Influencer.builder()
        .providerTypeId("GOOGLE")
        .providerMemberId(username!=null?username:LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
        .nickname(fullName.isEmpty() ? username : fullName)
        .gender(false)
        .birthday(LocalDate.now().minusYears(25))
        .email(username + "@instagram.com")
        .bio(biography.length() > 401 ? biography.substring(0, 401) : biography)
        .isDeleted(false)
        .build();
  }

  private PlatformAccount createPlatformAccountFromProfile(JsonNode profile, Integer influencerId) {
    String username = profile.path("username").asText();
    String fullName = profile.path("fullName").asText();
    String url = profile.path("url").asText();
    String profilePicUrl = profile.path("profilePicUrl").asText();

    File profileImageFile = null;
    if (!profilePicUrl.isEmpty()) {
      profileImageFile = imageService.downloadAndSaveProfileImage(profilePicUrl, username);
    }

    return PlatformAccount.builder()
        .influencerId(influencerId)
        .platformTypeId("INSTAGRAM")
        .externalAccountId(profile.path("id").asText())      // "58740544295"
        .accountNickname(username)
        .accountUrl(url)
        .accountProfileImageId(profileImageFile != null ? profileImageFile.getId() : null)
        //TODO : 카테고리는 DB에 있는걸로 넣기
        .categoryTypeId(1)
        .isDeleted(false)
        .deletedAt(LocalDateTime.now())
        .build();
  }

  private void saveProfileToDatabase(String jsonResults) {
    try {
      JsonNode results = objectMapper.readTree(jsonResults);
      JsonNode profile = results.get(0);

      //TODO : 지금은 더미데이터때문에 넣는거니까 나중에는 수정하기 = 나중에는 influencer랑 platfromAccount 생성할 db 저장 필요없으니까 빼기..
      // 1. DB 저장
      Influencer influencer = createInfluencerFromProfile(profile);
      Influencer savedInfluencer = influencerRepository.save(influencer);

      PlatformAccount platformAccount = createPlatformAccountFromProfile(profile, savedInfluencer.getId());
      platformAccountRepository.save(platformAccount);

      // 2. S3 저장
      saveProfileToS3(profile);

      log.info("DB 및 S3 저장 완료 - Influencer ID: {}", savedInfluencer.getId());

    } catch (Exception e) {
      log.error("DB/S3 저장 중 오류: ", e);
    }
  }

  private void saveProfileToS3(JsonNode profile) {
    try {
      String externalAccountId = profile.path("id").asText();        // "58740544295"
      String accountNickname = profile.path("username").asText();

      //TODO : DB에 있는걸로 바꾸기!!
      String categoryName = "IT";
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



}