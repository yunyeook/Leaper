package com.ssafy.spark.domain.crawling.youtube.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.spark.domain.business.content.dto.response.ContentResponse;
import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.service.ContentService;
import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import com.ssafy.spark.domain.spark.service.S3DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * YouTube 크롤링 데이터의 S3 저장을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeS3StorageService {

    private final S3DataService s3DataService;
    private final ObjectMapper objectMapper;
    private final ContentService contentService;

    /**
     * 비디오 정보에서 comments 필드를 완전히 제거한 VideoInfoResponse 생성
     */
    private VideoInfoResponse createVideoWithoutComments(VideoInfoWithCommentsResponse original) {
        VideoInfoResponse copy = new VideoInfoResponse();
        copy.setAccountNickname(original.getAccountNickname());
        copy.setExternalContentId(original.getExternalContentId());
        copy.setPlatformType(original.getPlatformType());
        copy.setContentType(original.getContentType());
        copy.setTitle(original.getTitle());
        copy.setDescription(original.getDescription());
        copy.setDurationSeconds(original.getDurationSeconds());
        copy.setContentUrl(original.getContentUrl());
        copy.setPublishedAt(original.getPublishedAt());
        copy.setTags(original.getTags());
        copy.setViewsCount(original.getViewsCount());
        copy.setLikesCount(original.getLikesCount());
        copy.setCommentsCount(original.getCommentsCount());
        copy.setThumbnailInfo(original.getThumbnailInfo());
        return copy;
    }

    /**
     * 댓글 데이터를 요청된 형태의 JSON으로 변환
     */
    private String createCommentsJsonFormat(VideoInfoWithCommentsResponse video) {
        try {
            Optional<Content> content =
                    contentService.findEntityByExternalContentIdAndPlatformType(video.getExternalContentId(), video.getPlatformType());

            java.util.Map<String, Object> commentsData = new java.util.HashMap<>();
            commentsData.put("contentId", content.get().getId());
            commentsData.put("platformAccountId", content.get().getPlatformAccount().getId());
            commentsData.put("contentUrl", video.getContentUrl());
            commentsData.put("commentsCount", video.getComments() != null ? video.getComments().size() : 0);

            java.util.List<java.util.Map<String, Object>> cleanComments = new java.util.ArrayList<>();
            if (video.getComments() != null) {
                for (VideoWithCommentsResponse.CommentInfo comment : video.getComments()) {
                    java.util.Map<String, Object> cleanComment = new java.util.HashMap<>();
                    cleanComment.put("accountNickname", comment.getAccountNickname());
                    cleanComment.put("externalCommentId", comment.getExternalCommentId());
                    cleanComment.put("text", comment.getText());
                    cleanComment.put("likesCount", comment.getLikesCount());
                    cleanComment.put("publishedAt", comment.getPublishedAt());
                    cleanComments.add(cleanComment);
                }
            }

            commentsData.put("comments", cleanComments);
            commentsData.put("crawledAt", java.time.LocalDateTime.now().toString());

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(commentsData);
        } catch (Exception e) {
            throw new RuntimeException("댓글 JSON 형태 변환 실패", e);
        }
    }

    /**
     * 채널의 전체 데이터를 S3에 저장
     */
    public Mono<String> saveChannelFullData(ChannelWithVideosResponse channelData) {
        return Mono.fromCallable(() -> {
            try {
                // 1. 채널 정보 저장
                String channelJson = objectMapper.writeValueAsString(channelData.getChannelInfo());
                String channelS3Url = s3DataService.saveYouTubeChannelInfo(channelData.getChannelInfo().getExternalAccountId(), channelJson);

                // 2. 각 비디오 정보 저장
                int savedVideos = 0;
                int savedComments = 0;

                for (VideoInfoWithCommentsResponse video : channelData.getVideos()) {
                    // 비디오 정보 저장 (comments 제거)
                    VideoInfoResponse videoWithoutComments = createVideoWithoutComments(video);
                    String videoJson = objectMapper.writeValueAsString(videoWithoutComments);
                    s3DataService.saveYouTubeVideoInfo(video.getExternalContentId(), videoJson);
                    savedVideos++;

                    // 댓글이 있으면 새로운 형태로 저장
                    if (video.getComments() != null && !video.getComments().isEmpty()) {
                        String commentsJson = createCommentsJsonFormat(video);
                        s3DataService.saveYouTubeComments(video.getExternalContentId(), commentsJson);
                        savedComments++;
                    }
                }

                log.info("채널 전체 데이터 S3 저장 완료: {} - 채널정보: {}, 비디오: {}개, 댓글: {}개",
                        channelData.getChannelInfo().getExternalAccountId(),
                        channelS3Url, savedVideos, savedComments);

                return channelS3Url;
            } catch (Exception e) {
                log.error("채널 전체 데이터 S3 저장 실패: {}",
                        channelData.getChannelInfo().getExternalAccountId(), e);
                throw new RuntimeException("채널 전체 데이터 S3 저장 실패", e);
            }
        });
    }

}