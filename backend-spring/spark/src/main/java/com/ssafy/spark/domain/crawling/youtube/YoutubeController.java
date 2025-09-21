package com.ssafy.spark.domain.crawling.youtube;

import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeApiService youtubeApiService;

    /**
     * 채널 정보 조회
     */
    @GetMapping("/channel/{externalAccountId}/info")
    public Mono<ChannelInfoResponse> getChannelInfo(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelInfoResponse(externalAccountId);
    }

    /**
     * 채널의 모든 영상 조회
     */
    @GetMapping("/channel/{externalAccountId}/videos")
    public Flux<VideoInfoResponse> getChannelVideos(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelVideosWithHandle(externalAccountId);
    }

    /**
     * 채널의 긴 영상만 조회
     */
    @GetMapping("/channel/{externalAccountId}/video-long")
    public Flux<VideoInfoResponse> getChannelLongVideos(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelLongVideosWithHandle(externalAccountId);
    }

    /**
     * 채널의 짧은 영상만 조회
     */
    @GetMapping("/channel/{externalAccountId}/video-short")
    public Flux<VideoInfoResponse> getChannelShortVideos(@PathVariable String externalAccountId) {
        return youtubeApiService.getChannelShortVideosWithHandle(externalAccountId);
    }

    /**
     * 특정 비디오 정보 조회
     */
    @GetMapping("/video/{videoId}/info")
    public Mono<VideoInfoResponse> getVideoInfo(@PathVariable("videoId") String videoId) {
        return youtubeApiService.getVideoInfoResponseWithHandle(videoId);
    }

    /**
     * 특정 비디오의 댓글 조회
     */
    @GetMapping("/video/{videoId}/comments")
    public Mono<VideoWithCommentsResponse> getVideoComments(
            @PathVariable("videoId") String videoId,
            @RequestParam(value = "maxResults", defaultValue = "20") Integer maxResults) {
        return youtubeApiService.getVideoWithComments(videoId, maxResults)
                .map(youtubeApiService::convertToVideoWithCommentsResponse);
    }

    /**
     * 특정 비디오 정보 + 댓글 함께 조회
     */
    @GetMapping("/video/{videoId}/info-with-comments")
    public Mono<VideoInfoWithCommentsResponse> getVideoInfoWithComments(
            @PathVariable("videoId") String videoId,
            @RequestParam(value = "maxComments", defaultValue = "50") Integer maxComments) {
        return youtubeApiService.getVideoInfoWithCommentsResponseWithHandle(videoId, maxComments);
    }

    /**
     * 채널 정보 + 채널의 모든 비디오 + 댓글 함께 조회
     */
    @GetMapping("/channel/{externalAccountId}/info-and-videos-with-comments")
    public Mono<ChannelWithVideosResponse> getChannelInfoAndVideosWithComments(
            @PathVariable("externalAccountId") String externalAccountId,
            @RequestParam(value = "maxCommentsPerVideo", defaultValue = "10") Integer maxCommentsPerVideo) {
        return youtubeApiService.getChannelWithVideosResponse(externalAccountId, maxCommentsPerVideo);
    }
}