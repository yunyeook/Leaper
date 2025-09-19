package com.ssafy.spark.domain.crawling.instagram;

import java.util.Arrays;
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

  private final ApifyService apifyService;

  /**
   * 1. 프로필 정보만 조회 GET /api/instagram/profile/humansofny
   */
  @GetMapping(value = "/profile/{username}", produces = "application/json")
  @ResponseBody
  public String getProfile(@PathVariable String username) {
    log.info("프로필 정보만 조회 요청: {}", username);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getProfileOnly(username);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("프로필 조회 완료: {}", username);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("프로필 조회 실패: ", e);
      return "{\"error\": \"프로필 조회 실패: " + e.getMessage() + "\"}";
    }
  }

  /**
   * 2. 게시물 정보만 조회 GET /api/instagram/posts/humansofny?limit=10
   */
  @GetMapping(value = "/posts/{username}", produces = "application/json")
  @ResponseBody
  public String getPosts(@PathVariable String username,
      @RequestParam(defaultValue = "10") int limit) {
    log.info("게시물 정보만 조회 요청: {} ({}개)", username, limit);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getPostsOnly(username, limit);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("게시물 조회 완료: {} ({}개)", username, limit);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("게시물 조회 실패: ", e);
      return "{\"error\": \"게시물 조회 실패: " + e.getMessage() + "\"}";
    }
  }

  /**
   * 3. 프로필 + 게시물 함께 조회 GET /api/instagram/all/humansofny?limit=10
   */
  @GetMapping(value = "/all/{username}", produces = "application/json")
  @ResponseBody
  public String getAll(@PathVariable String username,
      @RequestParam(defaultValue = "10") int limit) {
    log.info("프로필 + 게시물 함께 조회 요청: {} ({}개)", username, limit);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getProfileAndPosts(username, limit);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("전체 조회 완료: {} ({}개)", username, limit);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("전체 조회 실패: ", e);
      return "{\"error\": \"전체 조회 실패: " + e.getMessage() + "\"}";
    }
  }
  /**
   * 4. 해시태그로 게시물 조회
   * GET /api/instagram/hashtag/webscraping?limit=20
   */
  @GetMapping(value = "/hashtag/{hashtag}", produces = "application/json")
  @ResponseBody
  public String getHashtagPosts(@PathVariable String hashtag,
      @RequestParam(defaultValue = "20") int limit) {
    log.info("해시태그 게시물 조회 요청: #{} ({}개)", hashtag, limit);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getHashtagPosts(hashtag, limit);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("해시태그 조회 완료: #{} ({}개)", hashtag, limit);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("해시태그 조회 실패: ", e);
      return "{\"error\": \"해시태그 조회 실패: " + e.getMessage() + "\"}";
    }
  }
  /**
   * 5. 댓글 수집 (POST 요청 - 여러 URL 지원)
   * POST /api/instagram/comments
   * Body: {
   *   "directUrls": ["https://www.instagram.com/p/DCZlEDqy2to", "https://www.instagram.com/reel/DDIJAfeyemG"],
   *   "resultsLimit": 15,
   *   "includeNestedComments": false,
   *   "isNewestComments": false
   * }
   */


  /**
   * 6. 단일 게시물 댓글 수집 (GET 요청 - 편의 메서드)
   * GET /api/instagram/comments/single?postUrl=https://www.instagram.com/p/DCZlEDqy2to&limit=15
   */
  @GetMapping(value = "/comments/single", produces = "application/json")
  @ResponseBody
  public String getSinglePostComments(@RequestParam String postUrl,
      @RequestParam(defaultValue = "15") int limit) {
    log.info("단일 게시물 댓글 수집 요청: {} ({}개)", postUrl, limit);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getPostComments(postUrl, limit);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("단일 게시물 댓글 수집 완료: {}", postUrl);
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("단일 게시물 댓글 수집 실패: ", e);
      return "{\"error\": \"단일 게시물 댓글 수집 실패: " + e.getMessage() + "\"}";
    }
  }

  /**
   * 7. URL 파라미터로 여러 게시물 댓글 수집 (GET 요청)
   * GET /api/instagram/comments/multi?urls=url1,url2,url3&limit=15&includeNested=false&newest=false
   */
  @GetMapping(value = "/comments/multi", produces = "application/json")
  @ResponseBody
  public String getMultiPostComments(@RequestParam String urls,
      @RequestParam(defaultValue = "15") int limit,
      @RequestParam(defaultValue = "false") boolean includeNested,
      @RequestParam(defaultValue = "false") boolean newest) {

    // 쉼표로 구분된 URL들을 리스트로 변환
    List<String> urlList = Arrays.asList(urls.split(","));

    log.info("여러 게시물 댓글 수집 요청: {} 개 URL, {}개 댓글", urlList.size(), limit);

    try {
      // 동기 실행으로 변경하여 JSON 결과 직접 반환
      CompletableFuture<String> future = apifyService.getComments(urlList, limit, includeNested, newest);
      String jsonResult = future.get(); // 결과를 기다림

      log.info("여러 게시물 댓글 수집 완료: {} 개 URL", urlList.size());
      return jsonResult; // JSON 데이터 그대로 반환

    } catch (Exception e) {
      log.error("여러 게시물 댓글 수집 실패: ", e);
      return "{\"error\": \"여러 게시물 댓글 수집 실패: " + e.getMessage() + "\"}";
    }
  }


  /**
   * Apify API 연결 테스트
   */
  @GetMapping("/test-apify")
  public String testApify() {
    log.info("Apify 연결 테스트 요청");
    return apifyService.testApifyConnection();
  }



}