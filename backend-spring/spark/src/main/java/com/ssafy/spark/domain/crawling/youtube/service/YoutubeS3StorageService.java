package com.ssafy.spark.domain.crawling.youtube.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.spark.domain.business.content.entity.Content;
import com.ssafy.spark.domain.business.content.service.ContentService;
import com.ssafy.spark.domain.business.platformAccount.service.PlatformAccountService;
import com.ssafy.spark.domain.crawling.instagram.service.CrawlingDataSaveToS3Service;
import com.ssafy.spark.domain.crawling.youtube.dto.response.*;
import com.ssafy.spark.domain.insight.service.ReadToS3DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * YouTube 크롤링 데이터의 S3 저장을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeS3StorageService {

    private final ReadToS3DataService s3DataService;
    private final ObjectMapper objectMapper;
    private final ContentService contentService;
    private final PlatformAccountService platformAccountService;
    private final CrawlingDataSaveToS3Service crawlingDataSaveToS3Service;
    private final String platform = "youtube";

    /**
     * 비디오 정보에서 comments 필드를 완전히 제거한 VideoInfoResponse 생성
     * 썸네일 URL을 S3에 저장하고 accessKey 변경
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

        // 썸네일 처리: URL에서 다운로드하여 S3에 저장
        if (original.getThumbnailInfo() != null && original.getThumbnailInfo().getAccessKey() != null) {
            try {
                String thumbnailUrl = original.getThumbnailInfo().getAccessKey(); // 기존에는 YouTube URL
                String username = original.getAccountNickname(); // 채널명 사용
                String externalContentId = original.getExternalContentId();

                // S3에 썸네일 저장하고 새로운 accessKey 받기
                String s3AccessKey = s3DataService.saveYouTubeThumbnailFromUrl(username, externalContentId, thumbnailUrl);

                // 새로운 ThumbnailInfo 생성
                VideoInfoResponse.ThumbnailInfo newThumbnailInfo = new VideoInfoResponse.ThumbnailInfo();
                newThumbnailInfo.setAccessKey(s3AccessKey);
                newThumbnailInfo.setContentType("image/jpeg");

                copy.setThumbnailInfo(newThumbnailInfo);

                log.info("썸네일 S3 저장 완료: {} -> {}", thumbnailUrl, s3AccessKey);

            } catch (Exception e) {
                log.error("썸네일 S3 저장 실패: {}", original.getExternalContentId(), e);
                // 실패시 원본 thumbnailInfo 사용
                copy.setThumbnailInfo(original.getThumbnailInfo());
            }
        } else {
            copy.setThumbnailInfo(original.getThumbnailInfo());
        }

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
    public Mono<ChannelWithVideosResponse> saveChannelFullData(ChannelWithVideosResponse channelData) {
        return Mono.fromCallable(() -> {
            try {
                // 1. 채널 정보 처리 및 저장 (프로필 이미지 포함)
                log.info("채널 정보 저장(프로필 이미지 포함)");
                ChannelInfoResponse processedChannelInfo = processChannelInfoWithProfileImage(channelData.getChannelInfo());
                String channelJson = objectMapper.writeValueAsString(processedChannelInfo);
                crawlingDataSaveToS3Service.uploadProfileData(channelJson,platform,processedChannelInfo.getExternalAccountId());

                // 2. 각 비디오 정보 저장 및 썸네일 업데이트
                List<VideoInfoWithCommentsResponse> updatedVideos = new ArrayList<>();

                for (VideoInfoWithCommentsResponse video : channelData.getVideos()) {
                    // 비디오 썸네일 처리
                    VideoInfoWithCommentsResponse updatedVideo = processVideoWithThumbnail(video);
                    updatedVideos.add(updatedVideo);

                    // 비디오 정보 저장 (comments 제거)
                    VideoInfoResponse videoWithoutComments = createVideoWithoutComments(updatedVideo);
                    String videoJson = objectMapper.writeValueAsString(videoWithoutComments);
                    crawlingDataSaveToS3Service.uploadContentData(videoJson,platform,updatedVideo.getExternalContentId());

                    // 댓글이 있으면 새로운 형태로 저장
                    if (updatedVideo.getComments() != null && !updatedVideo.getComments().isEmpty()) {
                        String commentsJson = createCommentsJsonFormat(updatedVideo);
                        Optional<Content> content = contentService.findEntityByExternalContentIdAndPlatformType(updatedVideo.getExternalContentId(), updatedVideo.getPlatformType());
                        if (content.isPresent()) {
                            crawlingDataSaveToS3Service.uploadCommentData(commentsJson,platform,updatedVideo.getExternalContentId());
                        }
                    }
                }



                // 업데이트된 데이터로 응답 생성
                ChannelWithVideosResponse updatedResponse = new ChannelWithVideosResponse(processedChannelInfo, updatedVideos);

                return updatedResponse;
            } catch (Exception e) {
                log.error("채널 전체 데이터 S3 저장 실패: {}",
                        channelData.getChannelInfo().getExternalAccountId(), e);
                throw new RuntimeException("채널 전체 데이터 S3 저장 실패", e);
            }
        });
    }

    private VideoInfoWithCommentsResponse processVideoWithThumbnail(VideoInfoWithCommentsResponse video) {
        // 원본 비디오 정보 복사
        VideoInfoWithCommentsResponse copy = new VideoInfoWithCommentsResponse();
        copy.setAccountNickname(video.getAccountNickname());
        copy.setExternalContentId(video.getExternalContentId());
        copy.setPlatformType(video.getPlatformType());
        copy.setContentType(video.getContentType());
        copy.setTitle(video.getTitle());
        copy.setDescription(video.getDescription());
        copy.setDurationSeconds(video.getDurationSeconds());
        copy.setContentUrl(video.getContentUrl());
        copy.setPublishedAt(video.getPublishedAt());
        copy.setTags(video.getTags());
        copy.setViewsCount(video.getViewsCount());
        copy.setLikesCount(video.getLikesCount());
        copy.setCommentsCount(video.getCommentsCount());
        copy.setComments(video.getComments()); // 댓글 정보도 복사

        // 썸네일 처리: URL에서 다운로드하여 S3에 저장
        if (video.getThumbnailInfo() != null && video.getThumbnailInfo().getAccessKey() != null) {
            try {
                String thumbnailUrl = video.getThumbnailInfo().getAccessKey(); // 기존에는 YouTube URL
                String username = video.getAccountNickname(); // 채널명 사용
                String externalContentId = video.getExternalContentId();

                // S3에 썸네일 저장하고 새로운 accessKey 받기
                String s3AccessKey = s3DataService.saveYouTubeThumbnailFromUrl(username, externalContentId, thumbnailUrl);

                // 새로운 ThumbnailInfo 생성
                VideoInfoResponse.ThumbnailInfo newThumbnailInfo = new VideoInfoResponse.ThumbnailInfo();
                newThumbnailInfo.setAccessKey(s3AccessKey);
                newThumbnailInfo.setContentType("image/jpeg");

                copy.setThumbnailInfo(newThumbnailInfo);

                log.info("비디오 썸네일 S3 저장 완료: {} -> {}", thumbnailUrl, s3AccessKey);

            } catch (Exception e) {
                log.error("비디오 썸네일 S3 저장 실패: {}", video.getExternalContentId(), e);
                // 실패시 원본 thumbnailInfo 사용
                copy.setThumbnailInfo(video.getThumbnailInfo());
            }
        } else {
            copy.setThumbnailInfo(video.getThumbnailInfo());
        }

        return copy;
    }

    /**
     * 채널 정보의 프로필 이미지를 S3에 저장하고 accessKey 변경
     */
    private ChannelInfoResponse processChannelInfoWithProfileImage(ChannelInfoResponse original) {
        // 원본 정보 복사
        ChannelInfoResponse copy = new ChannelInfoResponse();
        copy.setExternalAccountId(original.getExternalAccountId());
        copy.setAccountNickname(original.getAccountNickname());
        copy.setCategoryName(original.getCategoryName());
        copy.setAccountUrl(original.getAccountUrl());
        copy.setFollowersCount(original.getFollowersCount());
        copy.setPostsCount(original.getPostsCount());
        copy.setCrawledAt(original.getCrawledAt());

        // 프로필 이미지 처리: URL에서 다운로드하여 S3에 저장
        if (original.getProfileImageInfo() != null && original.getProfileImageInfo().getAccessKey() != null) {
            try {
                log.info("processChannelInfoWithProfileImage : 프로필 이미지 s3 저장 시작");
                String profileImageUrl = original.getProfileImageInfo().getAccessKey(); // 기존에는 YouTube URL
                String username = original.getAccountNickname(); // 채널명 사용
                String externalAccountId = original.getExternalAccountId();

                // S3에 프로필 이미지 저장하고 accessKey, fileId 받기
                ReadToS3DataService.ProfileImageSaveResult saveResult = s3DataService.saveProfileImageFromUrl(username, profileImageUrl);

                // 새로운 ProfileImageInfo 생성
                ChannelInfoResponse.ProfileImageInfo newProfileImageInfo = new ChannelInfoResponse.ProfileImageInfo();
                newProfileImageInfo.setAccessKey(saveResult.getAccessKey());
                newProfileImageInfo.setContentType("image/jpeg");

                copy.setProfileImageInfo(newProfileImageInfo);

                // PlatformAccount의 accountProfileImageId 업데이트
                if (saveResult.getFileId() != null) {
                    try {
                        platformAccountService.updateAccountProfileImageId(externalAccountId, "youtube", saveResult.getFileId());
                        log.info("PlatformAccount 프로필 이미지 ID 업데이트 서비스 호출 완료: {} -> fileId={}",
                                externalAccountId, saveResult.getFileId());
                    } catch (Exception updateError) {
                        log.error("PlatformAccount 업데이트 실패: {}", externalAccountId, updateError);
                    }
                }

                log.info("프로필 이미지 S3 저장 완료: {} -> {}", profileImageUrl, saveResult.getAccessKey());

            } catch (Exception e) {
                log.error("프로필 이미지 S3 저장 실패: {}", original.getAccountNickname(), e);
                // 실패시 원본 profileImageInfo 사용
                copy.setProfileImageInfo(original.getProfileImageInfo());
            }
        } else {
            log.info("processChannelInfoWithProfileImage : 프로필 이미지 null");
            copy.setProfileImageInfo(original.getProfileImageInfo());
        }

        return copy;
    }

}