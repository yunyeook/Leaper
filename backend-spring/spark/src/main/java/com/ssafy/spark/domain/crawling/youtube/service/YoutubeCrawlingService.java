package com.ssafy.spark.domain.crawling.youtube.service;

import com.ssafy.spark.domain.business.content.service.ContentUpsertService;
import com.ssafy.spark.domain.business.influencer.service.InfluencerService;
import com.ssafy.spark.domain.business.platformAccount.service.PlatformAccountService;
import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * YouTube 크롤링과 S3 저장을 조합하는 통합 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeCrawlingService {

    private final YoutubeApiService youtubeApiService;
    private final YoutubeS3StorageService youtubeS3StorageService;
    private final ContentUpsertService contentUpsertService;
    private final InfluencerService influencerService;
    private final PlatformAccountService platformAccountService;

    /**
     * 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    public Mono<ChannelWithVideosResponse> getChannelFullDataAndSave(String externalAccountId, Integer maxCommentsPerVideo, Integer maxVideos, short categoryTypeId) {
        return youtubeApiService.getChannelWithVideosResponse(externalAccountId, maxCommentsPerVideo, maxVideos, categoryTypeId)
                .flatMap(channelData -> {
                    // 1. 인플루언서 및 PlatformAccount 먼저 생성
                    return Mono.fromCallable(() -> {
                        ChannelInfoResponse channelInfo = channelData.getChannelInfo();
                        String nickname = channelInfo.getAccountNickname();
                        String bio = channelInfo.getAccountNickname(); // 카테고리를 bio로 사용
                        Integer profileImageId = null; // 프로필 이미지는 S3 저장 후에 설정될 예정

                        // 인플루언서 생성
                        var influencer = influencerService.createInfluencer(nickname, bio, profileImageId);

                        // PlatformAccount 생성
                        var platformAccount = platformAccountService.createPlatformAccount(
                                influencer, externalAccountId, nickname, categoryTypeId);

                        log.info("인플루언서 및 PlatformAccount 생성 완료: {} (인플루언서 ID: {}, PlatformAccount ID: {})",
                                influencer.getNickname(), influencer.getId(), platformAccount.getId());

                        return influencer;
                    })
                    .doOnError(e -> log.error("인플루언서 또는 PlatformAccount 생성 실패", e))
                    // 2. Content 테이블에 Upsert (PlatformAccount 생성 후 실행)
                    .then(Mono.fromCallable(() ->
                            contentUpsertService.upsertContentsFromYouTubeVideos(channelData.getVideos(), externalAccountId)
                    ))
                    .doOnSuccess(contents -> log.info("Content Upsert 완료: {} 개", contents.size()))
                    .doOnError(e -> log.error("Content Upsert 실패", e))
                    // 3. S3 저장
                    .then(youtubeS3StorageService.saveChannelFullData(channelData))
                    .thenReturn(channelData);
                });
    }

}