package com.ssafy.spark.domain.crawling.instagram;

import com.ssafy.spark.domain.crawling.instagram.service.CommentService;
import com.ssafy.spark.domain.crawling.instagram.service.InstagramContentService;
import com.ssafy.spark.domain.crawling.instagram.service.ProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
public class InstagramController {

//  private final BaseApifyService apifyService;
  private  final ProfileService profileService;
  private final InstagramContentService contentService;
  private final CommentService commentService;

  /**
   * 1. 프로필 정보 생성 (카테고리 지정)
   */
//  @PostMapping(value = "/profile/{username}/category/{categoryTypeId}")
  @PostMapping(value = "/profile")
  @ResponseBody
//  public String getProfile(@PathVariable String username, @PathVariable Integer categoryTypeId) {
    public String getProfile() {

//    log.info("프로필 정보 생성 요청: {}, 카테고리: {}", username, categoryTypeId);
    List<String> usernames = List.of(
        "poorr_official",
        "contereve_",
        "t._.zzoon",
        "yya_ddak_",
        "dodoong.2",
        "mhseonbae",
        "plithus_toon",
        "rse1853"

    );
for(String username:usernames){
  try {
    CompletableFuture<String> future = profileService.getProfileOnly(username, 12);
    String jsonResult = future.get();
    log.info("프로필 생성 완료: {}", username);
  } catch (Exception e) {
    log.error("프로필 생성 실패: ", e);
//    return "{\"error\": \"프로필 생성 실패: " + e.getMessage() + "\"}";
  }

}
   return "성공";
  }


  /**
   * 1-2. 기존 인플루언서에 플랫폼 계정 연결 (카테고리 지정)
   */
  @PostMapping("/profile/link/{existingInfluencerId}/username/{username}/category/{categoryTypeId}")
  public String linkToExistingInfluencer(
      @PathVariable Integer existingInfluencerId,
      @PathVariable String username,
      @PathVariable Integer categoryTypeId) {

    try {
      log.info("기존 인플루언서에 연결 요청 - Influencer ID: {}, Username: {}, Category: {}",
          existingInfluencerId, username, categoryTypeId);
      CompletableFuture<String> future = profileService.linkPlatformAccountToExistingInfluencer( username, existingInfluencerId, categoryTypeId);
      String result = future.get();
      log.info("기존 인플루언서 연결 완료 - Influencer ID: {}", existingInfluencerId);
      return result;
    } catch (Exception e) {
      log.error("기존 인플루언서 연결 실패 - Influencer ID: {}, Username: {}", existingInfluencerId, username, e);
      return "{\"error\": \"연결 실패: " + e.getMessage() + "\"}";
    }
  }
  /**
   * 1-3. DB에 저장된 모든 Instagram 계정의 프로필 정보 일괄 업데이트(s3에만 저장)
   */
  @PostMapping("/profile/update/all")
  public String updateAllProfiles() {
    try {
      log.info("전체 Instagram 계정 프로필 일괄 업데이트 요청");
      profileService.updateAllRegisteredUsersProfiles();
      log.info("전체 Instagram 계정 프로필 일괄 업데이트 완료");
      return "{\"message\": \"전체 계정 프로필 업데이트가 완료되었습니다.\", \"status\": \"success\"}";
    } catch (Exception e) {
      log.error("전체 프로필 업데이트 실패: ", e);
      return "{\"error\": \"전체 프로필 업데이트 실패: " + e.getMessage() + "\", \"status\": \"failed\"}";
    }
  }
  /**
   * 2. 특정 사용자의 콘텐츠 수집
   */
  @GetMapping("/content/{username}")
  public String getContentsByUsername(@PathVariable String username) {
    try {
      log.info("콘텐츠 수집 요청: {}", username);
      CompletableFuture<String> future = contentService.getContentsByUsername(username);
      String result = future.get();
      log.info("콘텐츠 수집 완료: {}", username);
      return result;
    } catch (Exception e) {
      log.error("콘텐츠 수집 실패: {}", username, e);
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }

  /**
   * 2-2. DB에 저장된 모든 Instagram 계정의 콘텐츠 일괄 수집
   */
  @PostMapping("/content/collect/all")
  public String collectAllContent() {
    try {
      log.info("전체 Instagram 계정 콘텐츠 일괄 수집 요청");
      contentService.collectAllRegisteredUsersContent();
      log.info("전체 Instagram 계정 콘텐츠 일괄 수집 완료");
      return "{\"message\": \"전체 계정 콘텐츠 수집이 완료되었습니다.\", \"status\": \"success\"}";
    } catch (Exception e) {
      log.error("전체 콘텐츠 수집 실패: ", e);
      return "{\"error\": \"전체 콘텐츠 수집 실패: " + e.getMessage() + "\", \"status\": \"failed\"}";
    }
  }


  /**
   * 3. 특정 콘텐츠의 댓글 수집
   */
  @GetMapping("/comment/content/{contentId}")
  public String getCommentsByContentId(@PathVariable Integer contentId) {
    try {
      log.info("댓글 수집 요청 - Content ID: {}", contentId);
      CompletableFuture<String> future = commentService.getCommentsByContentId(contentId);
      String result = future.get();
      log.info("댓글 수집 완료 - Content ID: {}", contentId);
      return result;
    } catch (Exception e) {
      log.error("댓글 수집 실패 - Content ID: {}", contentId, e);
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }

  /**
   * 전체 콘텐츠 댓글 수집 (기존 API 활용)
   */
  @PostMapping("/comment/collect-all-batch-safe")
  public String collectAllCommentsBatchSafe(@RequestParam(defaultValue = "5") int batchSize) {
    try {
      log.info("안전한 배치 댓글 수집 요청 - 배치 크기: {}", batchSize);

      List<Integer> allContentIds = contentService.getInstagramContentIds();
      commentService.collectCommentsBatchUsingExistingApi(allContentIds, batchSize);

      return "{\"message\": \"안전한 배치 댓글 수집 완료\", \"status\": \"success\"}";
    } catch (Exception e) {
      log.error("안전한 배치 댓글 수집 실패: ", e);
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }


}