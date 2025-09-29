package com.ssafy.spark.domain.crawling.connect.service;

import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.repository.ContentRepository;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.crawling.connect.request.CrawlingRequest;
import com.ssafy.spark.domain.crawling.instagram.service.CommentService;
import com.ssafy.spark.domain.crawling.instagram.service.InstagramContentService;
import com.ssafy.spark.domain.crawling.instagram.service.ProfileService;
import com.ssafy.spark.domain.crawling.youtube.service.YoutubeCrawlingService;
import com.ssafy.spark.domain.insight.service.AccountInsightService;
import com.ssafy.spark.domain.insight.service.AccountPopularContentService;
import com.ssafy.spark.domain.insight.service.S3FolderService;
import com.ssafy.spark.domain.insight.service.SparkTypeInsightService;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlingServiceInstagram implements CrawlingService {
  private final ProfileService profileService;
  private final InstagramContentService contentService;
  private final CommentService commentService;
  private final ContentRepository contentRepository;
  private final PlatformAccountRepository platformAccountRepository;
  private final AccountInsightService sparkAccountInsightService;
  private final AccountPopularContentService sparkAccountPopularContentService;
  private final SparkTypeInsightService sparkTypeInsightService;
  private final YoutubeCrawlingService youtubeCrawlingService;
  private final S3FolderService s3FolderService;



  @Async
  public void startCrawlingAsync(CrawlingRequest request) {
    String username = request.getAccountNickname();
    Integer platformAccountId = request.getPlatformAccountId();
    String platformTypeId = request.getPlatformTypeId(); //instagram , youtube
    PlatformAccount platformAccount = platformAccountRepository.findById(platformAccountId).get();

    //일단 해당 경로 폴더 생성함.(있어도 상관 없음)
    s3FolderService.createDateFolders(platformTypeId, LocalDate.now());
    //youtube 연동시
    if (platformTypeId.equals("youtube")) {
      youtubeCrawlingService.getChannelFullDataAndSave(platformAccountId, 20, 20)
          .doOnSuccess(channelData -> {
            log.info("유튜브 채널 크롤링 및 S3 저장 완료: {}", channelData.getChannelInfo().getAccountNickname());
            // S3 저장 끝났으니 Spark 인사이트 실행
            try {
              generateInsightsAfterCrawling(request);
            } catch (Exception e) {
              log.error("유튜브 인사이트 생성 실패 - accountId={}", platformAccountId, e);
            }
          })
          .doOnError(e -> log.error("유튜브 크롤링 실패 - accountId={}", platformAccountId, e))
          .subscribe(); // ✅ 꼭 subscribe() 해야 실행됩니다
    }
    else if (platformTypeId.equals("instagram")) {

    try {
      // 1. 프로필 수집
       profileService.profileCrawling(platformAccount);
      log.info("프로필 생성 완료: {}", username);
    } catch (Exception e) {
      log.error("프로필 생성 실패: {}", username, e);
    }

    try {
      // 2. 콘텐츠 수집 - 오늘 이미 수집됐는지 확인
      LocalDate today = LocalDate.now();

      boolean hasContentToday = (platformAccount != null) &&
          contentRepository.existsByPlatformAccountAndSnapshotDate(platformAccount, today);

      if (hasContentToday) {
        log.info("계정 {} - 오늘 이미 수집됨, 콘텐츠 수집 건너뜀", username);
      } else {
        log.info("콘텐츠 수집 요청: {}", username);
        CompletableFuture<String> contentFuture = contentService.contentCrawling(username);

        log.info("콘텐츠 수집 완료: {}", username);
      }
    } catch (Exception e) {
      log.error("콘텐츠 수집 실패: {}", username, e);
    }

    // 댓글 수집은 콘텐츠가 있으면 진행
    try {
      List<Content> contents = contentRepository.findByPlatformAccountId(platformAccountId);
      log.info("컨텐츠 갯수: {}", contents.size());

      if (!contents.isEmpty()) {
        CompletableFuture<String> commentFuture = commentService.getCommentsByContents(username,
            contents);
        log.info("댓글 수집 완료: {}", username);
      } else {
        log.warn("댓글 수집 건너뜀: 수집된 콘텐츠가 없음");
      }
    } catch (Exception e) {
      log.error("댓글 수집 실패: {}", username, e);
    }

    //계정과 컨텐츠 크롤링 후 인사이트 생성
    generateInsightsAfterCrawling(request);
  }
  }

  private void generateInsightsAfterCrawling(CrawlingRequest request) {
    try {
      log.info("인사이트 생성 시작: accountId={}", request.getPlatformAccountId());

      sparkAccountInsightService.generateDailyAccountInsight(
          request.getPlatformTypeId(),  // String platformType
          LocalDate.now(),             // LocalDate targetDate
          request.getPlatformAccountId() // Integer specificPlatformAccountId
      );

      sparkAccountPopularContentService.generateAccountPopularContent(
          request.getPlatformTypeId(),
          LocalDate.now(),
          request.getPlatformAccountId()
      );
      sparkTypeInsightService.generateDailyTypeInsight(  request.getPlatformTypeId(),
          LocalDate.now(),
          request.getPlatformAccountId());

      log.info("인사이트 생성 완료: accountId={}", request.getPlatformAccountId());

    } catch (Exception e) {
      log.error("인사이트 생성 실패: accountId={}", request.getPlatformAccountId(), e);
    }
  }}