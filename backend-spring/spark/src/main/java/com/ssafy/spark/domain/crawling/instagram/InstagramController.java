package com.ssafy.spark.domain.crawling.instagram;

import com.ssafy.spark.domain.crawling.instagram.service.CommentService;
import com.ssafy.spark.domain.crawling.instagram.service.ContentService;
import com.ssafy.spark.domain.crawling.instagram.service.ProfileService;
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
  private final ContentService contentService;
  private final CommentService commentService;

  /**
   * 1. 프로필 정보만 조회 GET /api/instagram/profile/humansofny
   */
  @GetMapping(value = "/profile/{username}", produces = "application/json")
  @ResponseBody
  public String getProfile(@PathVariable String username) {
    log.info("프로필 정보만 조회 요청: {}", username);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = profileService.getProfileOnly(username);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("프로필 조회 완료: {}", username);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("프로필 조회 실패: ", e);
      return "{\"error\": \"프로필 조회 실패: " + e.getMessage() + "\"}";
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


}