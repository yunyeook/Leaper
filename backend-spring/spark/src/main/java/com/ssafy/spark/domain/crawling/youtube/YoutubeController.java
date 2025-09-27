package com.ssafy.spark.domain.crawling.youtube;

import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import com.ssafy.spark.domain.crawling.youtube.service.YoutubeApiService;
import com.ssafy.spark.domain.crawling.youtube.service.YoutubeCrawlingService;
import com.ssafy.spark.domain.spark.service.S3DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
@Slf4j
public class YoutubeController {

    private final YoutubeApiService youtubeApiService;
    private final YoutubeCrawlingService youtubeCrawlingService;
    private final S3DataService s3DataService;

    /**
     * Influencer + platformAcount 생성 후 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    @PostMapping("/channel/{externalAccountId}/full-crawl")
    public Mono<ChannelWithVideosResponse> getNewChannelFullDataAndSave(
            @PathVariable("externalAccountId") String externalAccountId,
            @RequestParam(value = "maxCommentsPerVideo", defaultValue = "20") Integer maxCommentsPerVideo,
            @RequestParam(value = "maxVideos", defaultValue = "20") Integer maxVideos,
            @RequestParam(value = "categoryTypeId", defaultValue = "12") short categoryTypeId) {
        return youtubeCrawlingService.getNewChannelFullDataAndSave(externalAccountId, maxCommentsPerVideo, maxVideos, categoryTypeId);
    }

    /**
     * platformAccount에 저장된 Youtube 채널들의 모든 데이터 조회 및 S3 저장 (일괄 크롤링)
     * 백그라운드에서 비동기 처리하고 즉시 응답 반환
     */
    @PostMapping("/channel/full-crawl")
    public Map<String, Object> getAllChannelFullDataAndSave(
            @RequestParam(value = "maxCommentsPerVideo", defaultValue = "20") Integer maxCommentsPerVideo,
            @RequestParam(value = "maxVideos", defaultValue = "20") Integer maxVideos) {

        // 백그라운드에서 비동기 실행
        executeBackgroundCrawling(maxCommentsPerVideo, maxVideos);

        // 즉시 응답 반환
        Map<String, Object> result = new HashMap<>();
        result.put("status", "crawling started in background");
        result.put("message", "크롤링이 백그라운드에서 시작되었습니다. 진행 상황은 로그를 확인해주세요.");
        result.put("timestamp", java.time.LocalDateTime.now());
        return result;
    }

    /**
     * 기존 PlatformAccount ID 기반 채널의 모든 데이터 조회 및 S3 저장 (Upsert 크롤링)
     */
    @PostMapping("/exist-channel/{platformAccountId}/full-crawl")
    public Mono<ChannelWithVideosResponse> getChannelFullDataAndSave(
            @PathVariable("platformAccountId") Integer platformAccountId,
            @RequestParam(value = "maxCommentsPerVideo", defaultValue = "20") Integer maxCommentsPerVideo,
            @RequestParam(value = "maxVideos", defaultValue = "20") Integer maxVideos) {
        return youtubeCrawlingService.getChannelFullDataAndSave(platformAccountId, maxCommentsPerVideo, maxVideos);
    }

    /**
     * 백그라운드에서 실제 크롤링 실행
     */
    @Async
    public void executeBackgroundCrawling(Integer maxCommentsPerVideo, Integer maxVideos) {
        youtubeCrawlingService.getAllChannelFullDataAndSave(maxCommentsPerVideo, maxVideos)
                .doOnSuccess(v -> log.info("백그라운드 크롤링 작업 완료"))
                .doOnError(e -> log.error("백그라운드 크롤링 작업 실패", e))
                .subscribe();
    }

    /**
     * util: 채널 닉네임(@handle)으로 채널 정보 검색
     */
    @GetMapping("/search/channel")
    public Mono<ChannelInfoResponse> searchChannelByHandle(@RequestParam("handle") String handle) {
        return youtubeApiService.searchChannelByHandle(handle);
    }

    /**
     * util: 채널 정보 조회 (채널 ID로)
     */
    @GetMapping("/channel/{externalAccountId}/info")
    public Mono<ChannelInfoResponse> getChannelInfo(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelInfoResponse(externalAccountId);
    }

    /**
     * util: 채널의 모든 영상 조회
     */
    @GetMapping("/channel/{externalAccountId}/videos")
    public Flux<VideoInfoResponse> getChannelVideos(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelVideosWithHandle(externalAccountId);
    }

    /**
     * util: 특정 비디오의 댓글 조회
     */
    @GetMapping("/video/{videoId}/comments")
    public Mono<VideoWithCommentsResponse> getVideoComments(
            @PathVariable("videoId") String videoId,
            @RequestParam(value = "maxResults", defaultValue = "20") Integer maxResults) {
        return youtubeApiService.getVideoWithComments(videoId, maxResults)
                .map(youtubeApiService::convertToVideoWithCommentsResponse);
    }

    // ========== S3 데이터 조회 API ==========

    /**
     * util : 오늘 날짜 S3 폴더에 저장된 YouTube 데이터 파일 목록 조회
     */
    @GetMapping("/s3/today-summary")
    public Map<String, Object> getTodaySummary() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> result = new HashMap<>();
        result.put("date", today);

        try {
            // 오늘 날짜의 각 타입별 파일 목록 조회
            List<String> channelFiles = s3DataService.listYouTubeChannelFiles(today);
            List<String> videoFiles = s3DataService.listYouTubeVideoFiles(today);
            List<String> commentFiles = s3DataService.listYouTubeCommentFiles(today);

            Map<String, Object> files = new HashMap<>();
            files.put("channels", channelFiles);
            files.put("videos", videoFiles);
            files.put("comments", commentFiles);

            result.put("files", files);
            result.put("summary", Map.of(
                "totalChannels", channelFiles.size(),
                "totalVideos", videoFiles.size(),
                "totalComments", commentFiles.size(),
                "totalFiles", channelFiles.size() + videoFiles.size() + commentFiles.size()
            ));

        } catch (Exception e) {
            result.put("error", "S3 파일 목록 조회 실패: " + e.getMessage());
        }

        return result;
    }

    /**
     * util : S3에서 특정 파일의 내용 조회
     */
    @GetMapping("/s3/file-content")
    public String getFileContent(@RequestParam("accessKey") String accessKey) {
        try {
            // 파일 존재 여부 확인
            if (!s3DataService.doesFileExist(accessKey)) {
                return "파일이 존재하지 않습니다: " + accessKey;
            }

            // 파일 내용 그대로 반환
            return s3DataService.getFileContent(accessKey);

        } catch (Exception e) {
            return "파일 내용 조회 실패: " + e.getMessage();
        }
    }
}