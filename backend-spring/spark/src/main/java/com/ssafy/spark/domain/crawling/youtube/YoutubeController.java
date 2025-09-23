package com.ssafy.spark.domain.crawling.youtube;

import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import com.ssafy.spark.domain.crawling.youtube.service.YoutubeApiService;
import com.ssafy.spark.domain.crawling.youtube.service.YoutubeCrawlingService;
import com.ssafy.spark.domain.spark.service.S3DataService;
import lombok.RequiredArgsConstructor;
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
public class YoutubeController {

    private final YoutubeApiService youtubeApiService;
    private final YoutubeCrawlingService youtubeCrawlingService;
    private final S3DataService s3DataService;

    /**
     * 채널의 모든 데이터 조회 및 S3 저장 (원스톱 크롤링)
     */
    @PostMapping("/channel/{externalAccountId}/full-crawl")
    public Mono<ChannelWithVideosResponse> getChannelFullDataAndSave(
            @PathVariable("externalAccountId") String externalAccountId,
            @RequestParam(value = "maxCommentsPerVideo", defaultValue = "20") Integer maxCommentsPerVideo,
            @RequestParam(value = "maxVideos", defaultValue = "20") Integer maxVideos,
            @RequestParam(value = "categoryTypeId", defaultValue = "1") short categoryTypeId) {
        return youtubeCrawlingService.getChannelFullDataAndSave(externalAccountId, maxCommentsPerVideo, maxVideos, categoryTypeId);
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