package com.ssafy.spark.domain.crawling.youtube.service;

import com.ssafy.spark.domain.business.content.service.ContentUpsertService;
import com.ssafy.spark.domain.business.influencer.service.InfluencerService;
import com.ssafy.spark.domain.business.platformAccount.service.PlatformAccountService;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

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
    private final PlatformAccountRepository platformAccountRepository;

    /**
     * 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    public Mono<ChannelWithVideosResponse> getNewChannelFullDataAndSave(String externalAccountId, Integer maxCommentsPerVideo, Integer maxVideos, short categoryTypeId) {
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
                            // 2. 데이터 Upsert 공통 로직 실행 (PlatformAccount 생성 후 실행)
                            .then(upsertData(channelData, externalAccountId));
                });
    }

    /**
     * 모든 YOUTUBE 플랫폼 계정의 채널 데이터 조회 및 S3 저장 (일괄 크롤링)
     */
    public Mono<Void> getAllChannelFullDataAndSave(Integer maxCommentsPerVideo, Integer maxVideos) {
        log.info("모든 YOUTUBE 플랫폼 계정 크롤링 시작 - maxCommentsPerVideo: {}, maxVideos: {}",
                maxCommentsPerVideo, maxVideos);

        // 성공/실패 계정 추적용 리스트
        List<String> successAccounts = new ArrayList<>();
        List<String> failedAccounts = new ArrayList<>();

        // 1. YOUTUBE 플랫폼의 모든 계정 조회
        return Mono.fromCallable(() -> platformAccountService.getAllYoutubePlatformAccounts())
                .doOnSuccess(accounts -> log.info("YOUTUBE 플랫폼 계정 {} 개 조회", accounts.size()))
                .flatMapMany(Flux::fromIterable)  // List를 Flux로 변환
                .flatMap(account -> {
                    String externalAccountId = account.getExternalAccountId();
                    short categoryTypeId = account.getCategoryType().getId();

                    log.info("계정 크롤링 시작 - externalAccountId: {}, categoryTypeId: {}",
                            externalAccountId, categoryTypeId);

                    // 2. 각 계정별로 채널 데이터 조회 및 저장
                    return youtubeApiService.getChannelWithVideosResponse(externalAccountId, maxCommentsPerVideo, maxVideos, categoryTypeId)
                            .flatMap(channelData -> {
                                // 데이터 Upsert 공통 로직 실행
                                return upsertData(channelData, externalAccountId).then();
                            })
                            .doOnSuccess(v -> {
                                synchronized (successAccounts) {
                                    successAccounts.add(externalAccountId);
                                }
                                log.info("계정 처리 성공 - {}", externalAccountId);
                            })
                            .doOnError(e -> log.error("채널 데이터 조회 실패 - 계정: {}", externalAccountId, e))
                            .onErrorResume(e -> {
                                synchronized (failedAccounts) {
                                    failedAccounts.add(externalAccountId);
                                }
                                log.warn("계정 {} 처리 중 오류 발생, 다음 계정으로 진행", externalAccountId, e);
                                return Mono.empty(); // 오류가 발생해도 다음 계정 처리 계속
                            });
                })
                .then()
                .doOnSuccess(v -> {
                    // 최종 결과 로그 출력
                    int totalAccounts = successAccounts.size() + failedAccounts.size();
                    log.info("=== YOUTUBE 플랫폼 계정 크롤링 완료 ===");
                    log.info("전체 계정: {} 개", totalAccounts);
                    log.info("성공한 계정: {} 개", successAccounts.size());
                    log.info("실패한 계정: {} 개", failedAccounts.size());

                    if (!failedAccounts.isEmpty()) {
                        log.warn("실패한 계정 목록: {}", failedAccounts);
                    }
                })
                .doOnError(e -> log.error("YOUTUBE 플랫폼 계정 크롤링 중 오류 발생", e));
    }

    /**
     * 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    public Mono<ChannelWithVideosResponse> getChannelFullDataAndSave(Integer platformAccountId, Integer maxCommentsPerVideo, Integer maxVideos) {
        // 1. PlatformAccount ID로 기존 PlatformAccount 조회
        return Mono.fromCallable(() -> {
                    var platformAccount = platformAccountRepository.findById(platformAccountId)
                            .orElseThrow(() -> new IllegalArgumentException("플랫폼 계정을 찾을 수 없습니다: " + platformAccountId));

                    log.info("기존 PlatformAccount 조회 완료: ID={}, externalAccountId={}",
                            platformAccount.getId(), platformAccount.getExternalAccountId());

                    return platformAccount;
                })
                .flatMap(platformAccount ->
                    youtubeApiService.getChannelWithVideosResponse(platformAccount.getExternalAccountId(), maxCommentsPerVideo, maxVideos, platformAccount.getCategoryType().getId())
                        .flatMap(channelData -> {
                            // 2. 데이터 Upsert 공통 로직 실행
                            return upsertData(channelData, platformAccount.getExternalAccountId());
                        })
                );
    }

    /**
     * 유튜브 채널 데이터 Upsert 공통 로직 (Content → S3 → 썸네일 → 인사이트)
     */
    private Mono<ChannelWithVideosResponse> upsertData(ChannelWithVideosResponse channelData, String externalAccountId) {
        log.info("YouTube 데이터 Upsert 시작 - 계정: {}", externalAccountId);

        return Mono.fromCallable(() -> {
                    // 1. Content 테이블에 Upsert
                    return contentUpsertService.upsertContentsFromYouTubeVideos(channelData.getVideos(), externalAccountId);
                })
                .doOnSuccess(contents -> log.info("Content Upsert 완료 - 계정: {}, {} 개",
                        externalAccountId, contents.size()))
                .doOnError(e -> log.error("Content Upsert 실패 - 계정: {}", externalAccountId, e))
                // 2. S3 저장
                .then(youtubeS3StorageService.saveChannelFullData(channelData))
                .doOnSuccess(v -> log.info("S3 저장 완료 - 계정: {}", externalAccountId))
                .doOnError(e -> log.error("S3 저장 실패 - 계정: {}", externalAccountId, e))
                .flatMap(updatedChannelData -> Mono.fromCallable(() -> {
                    // 3. 썸네일 ID 업데이트 (S3 저장 후, 업데이트된 데이터 사용)
                    log.info("S3 저장 후 업데이트된 데이터로 썸네일 ID 업데이트 시작 - 계정: {}", externalAccountId);
                    contentUpsertService.updateContentThumbnailIdAndDescription(updatedChannelData.getVideos(), externalAccountId);
                    return updatedChannelData;
                }))
                .doOnSuccess(v -> log.info("썸네일 ID 업데이트 완료 - 계정: {}", externalAccountId))
                .doOnError(e -> log.error("썸네일 ID 업데이트 실패 - 계정: {}", externalAccountId, e));
    }




}