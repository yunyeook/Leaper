package com.ssafy.spark.domain.crawling.youtube.service;

import com.ssafy.spark.domain.business.content.service.ContentUpsertService;
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

    /**
     * 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    public Mono<ChannelWithVideosResponse> getChannelFullDataAndSave(String externalAccountId, Integer maxCommentsPerVideo) {
        return youtubeApiService.getChannelWithVideosResponse(externalAccountId, maxCommentsPerVideo)
                .flatMap(channelData -> {
                    // 1. Content 테이블에 Upsert (S3 저장 전에 먼저 실행)
                    return Mono.fromCallable(() ->
                            contentUpsertService.upsertContentsFromYouTubeVideos(channelData.getVideos(), externalAccountId)
                    )
                    .doOnSuccess(contents -> log.info("Content Upsert 완료: {} 개", contents.size()))
                    .doOnError(e -> log.error("Content Upsert 실패", e))
                    // 2. S3 저장
                    .then(youtubeS3StorageService.saveChannelFullData(channelData))
                    .thenReturn(channelData);
                });
    }

}