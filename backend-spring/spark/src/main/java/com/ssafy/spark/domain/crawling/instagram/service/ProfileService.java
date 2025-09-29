package com.ssafy.spark.domain.crawling.instagram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.spark.domain.business.file.entity.File;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.crawling.instagram.dto.ProfileRawData;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService extends BaseApifyService {

  private final PlatformAccountRepository platformAccountRepository;
  private final CrawlingDataSaveToS3Service s3Service;
  private final ImageService imageService;
  private final String platformType = "instagram";

  /**
   * 모든 계정 프로필 크롤링 및 저장
   */
  public void allProfilesCrawling() {
    try {
      log.info("DB에서 Instagram 계정 목록 조회 시작");
      List<PlatformAccount> accounts = platformAccountRepository.findByPlatformTypeIdAndIsDeleted("INSTAGRAM", false);

      log.info("업데이트할 Instagram 계정 수: {}", accounts.size());
      if (accounts.isEmpty()) return;

      for (PlatformAccount account : accounts) {
        try {
          log.info("프로필 업데이트 시작 - 계정: {}, ID: {}", account.getAccountNickname(), account.getId());
          profileCrawling(account);
          Thread.sleep(5000); // API rate limit 대응
          log.info("프로필 업데이트 완료 - 계정: {}", account.getAccountNickname());
        } catch (Exception e) {
          log.error("프로필 업데이트 실패 - 계정: {}", account.getAccountNickname(), e);
        }
      }

      log.info("전체 Instagram 계정 프로필 업데이트 완료");
    } catch (Exception e) {
      log.error("프로필 일괄 업데이트 중 오류", e);
      throw new RuntimeException("프로필 일괄 업데이트 실패", e);
    }
  }

  /**
   * 단일 계정 프로필 크롤링 및 s3저장
   */
  public String profileCrawling(PlatformAccount platformAccount) {
    try {
      String username = platformAccount.getAccountNickname();
      log.info("프로필 업데이트 시작: {}", username);

      String runId = executeProfileScraper(username);
      String jsonResults = waitForRunCompletion(runId);

      JsonNode results = objectMapper.readTree(jsonResults);
      if (results.isEmpty()) {
        log.warn("프로필 결과가 비어있습니다 - 계정: {}", username);
        return "{}";
      }

      JsonNode profile = results.get(0);
      saveProfile(profile, platformAccount);

      return jsonResults;
    } catch (Exception e) {
      log.error("프로필 업데이트 실패 - 계정: {}", platformAccount.getAccountNickname(), e);
      throw new RuntimeException("프로필 업데이트 실패: " + platformAccount.getAccountNickname(), e);
    }
  }


  /**
   * 프로필 저장
   */
  private void saveProfile(JsonNode profile, PlatformAccount platformAccount) {
    try {
      String externalAccountId = profile.path("id").asText();
      String accountNickname = profile.path("username").asText();
      String categoryName = platformAccount.getCategoryType().getCategoryName();
      String accountUrl = profile.path("url").asText();
      int followersCount = profile.path("followersCount").asInt();
      int postsCount = profile.path("postsCount").asInt();
      String profilePicUrl = profile.path("profilePicUrl").asText();

      // 프로필 이미지가 없을 경우 저장
      if (platformAccount.getAccountProfileImageId() == null) {
        File profileImageFile = imageService.downloadAndSaveProfileImage(profilePicUrl, accountNickname);
        platformAccount.setAccountProfileImageId(profileImageFile.getId());
        platformAccountRepository.save(platformAccount);
      }

      // JSON 데이터 생성
      ProfileRawData rawData = ProfileRawData.builder()
          .externalAccountId(externalAccountId)
          .accountNickname(accountNickname.isEmpty() ? externalAccountId : accountNickname)
          .categoryName(categoryName)
          .accountUrl(accountUrl)
          .followersCount(followersCount)
          .postsCount(postsCount)
          .crawledAt(LocalDateTime.now().toString())
          .build();

      String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rawData);
      
      // JSON + Parquet 저장
      var paths = s3Service.uploadProfileData(jsonData, platformType, accountNickname);
      
      log.info("프로필 저장 완료 - 계정: {}", accountNickname);
      log.info("  - JSON: {}", paths.get("json"));
      log.info("  - Parquet: {}", paths.get("parquet"));
    } catch (Exception e) {
      log.error("S3 저장 중 오류 - 계정: {}", platformAccount.getAccountNickname(), e);
    }
  }
}
