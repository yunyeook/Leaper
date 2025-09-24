package com.ssafy.spark.domain.crawling.service;

import com.ssafy.spark.domain.crawling.dto.request.CrawlingRequest;
import com.ssafy.spark.domain.crawling.instagram.service.CommentService;
import com.ssafy.spark.domain.crawling.instagram.service.ContentService;
import com.ssafy.spark.domain.crawling.instagram.service.ProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlingService {
  private final ProfileService profileService;
  private final ContentService contentService;
  private final CommentService commentService;

  @Async
  public void startCrawlingAsync(CrawlingRequest request) {
    try {
      // 1. 프로필 수집
      CompletableFuture<String> profileFuture = profileService.getProfileOnly(request.getAccountNickname(), request.getCategoryTypeId());
      profileFuture.get();
      log.info("프로필 생성 완료: {}", request.getAccountNickname());
    } catch (Exception e) {
      log.error("프로필 생성 실패: ", e);
    }

    try {
      // 2. 콘텐츠 수집
      log.info("콘텐츠 수집 요청: {}", request.getAccountNickname());
      CompletableFuture<String> contentFuture = contentService.getContentsByUsername(request.getAccountNickname());
      contentFuture.get(); // 콘텐츠 수집 완료 대기
      log.info("콘텐츠 수집 완료: {}", request.getAccountNickname());
    } catch (Exception e) {
      log.error("콘텐츠 수집 실패: {}", request.getAccountNickname(), e);
    }

    try {
      // 3. 댓글 수집
      log.info("댓글 수집 시작: {}", request.getAccountNickname());

      // 기존 Instagram 콘텐츠 ID들 조회
      List<Integer> contentIds = contentService.getInstagramPlaformAccountContentIds(request.getPlatformAccountId());

      if (!contentIds.isEmpty()) {
        // Integer -> String 변환
        List<String> contentIdStrings = contentIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList());

        CompletableFuture<String> commentFuture = commentService.getCommentsByContentIds(
            request.getAccountNickname(), contentIdStrings);
        commentFuture.get();
        log.info("댓글 수집 완료: {}", request.getAccountNickname());
      } else {
        log.warn("댓글 수집 건너뜀: 수집된 콘텐츠가 없음");
      }
    } catch (Exception e) {
      log.error("댓글 수집 실패: {}", request.getAccountNickname(), e);
    }
  }
}