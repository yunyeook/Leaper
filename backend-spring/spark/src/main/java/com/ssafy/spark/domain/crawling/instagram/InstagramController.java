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

  private  final ProfileService profileService;
  private final InstagramContentService contentService;
  private final CommentService commentService;

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
   * 전체 콘텐츠 댓글 배치 수집 (기존 API 활용)
   */
  @PostMapping("/comment/collect-all-batch-safe")
  public String collectAllCommentsBatchSafe() {
    try {
  commentService.collectAllContentComments();

      return "{\"message\": \" 댓글 수집 완료\", \"status\": \"success\"}";
    } catch (Exception e) {
      log.error(" 댓글 수집 실패: ", e);
      return "{\"error\": \"" + e.getMessage() + "\"}";
    }
  }


}