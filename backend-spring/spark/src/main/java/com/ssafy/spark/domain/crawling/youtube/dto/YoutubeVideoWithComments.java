package com.ssafy.spark.domain.crawling.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeVideoWithComments {
    private String videoId;
    private String title;
    private String description;
    private String publishedAt;
    private String channelTitle;
    private String channelId;
    private String thumbnailUrl;
    private Integer thumbnailWidth;  // 썸네일 가로 크기
    private Integer thumbnailHeight; // 썸네일 세로 크기
    private Integer durationSeconds;
    private String dimension; // "2d" for normal videos, "2d" for shorts (YouTube API doesn't distinguish by dimension alone)
    private Long viewsCount;
    private Long likesCount;
    private Long commentsCount;
    private List<String> tags;
    private List<CommentInfo> comments;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommentInfo {
        private String commentId;
        private String authorName;
        private String authorProfileImage;
        private String commentText;
        private Integer likeCount;
        private String publishedAt;
        private String updatedAt;
        private Integer replyCount;
    }

    // YouTubeVideo에서 변환하는 생성자
    public static YoutubeVideoWithComments fromYouTubeVideo(YoutubeVideo video) {
        YoutubeVideoWithComments result = new YoutubeVideoWithComments();

        // 비디오 ID 추출
        String videoId = null;
        if (video.getSnippet().getResourceId() != null) {
            videoId = video.getSnippet().getResourceId().getVideoId();
        } else {
            videoId = video.getVideoId();
        }

        result.setVideoId(videoId);
        result.setTitle(video.getSnippet().getTitle());
        result.setDescription(video.getSnippet().getDescription());
        result.setPublishedAt(video.getSnippet().getPublishedAt());
        result.setChannelTitle(video.getSnippet().getChannelTitle());
        result.setChannelId(video.getSnippet().getChannelId());
        result.setDurationSeconds(parseDuration(video.getContentDetails() != null ? video.getContentDetails().getDuration() : null));

        // 비디오 차원 정보 설정 (dimension)
        if (video.getContentDetails() != null) {
            result.setDimension(video.getContentDetails().getDimension());
        }

        // 통계 정보 설정
        if (video.getStatistics() != null) {
            result.setViewsCount(parseLong(video.getStatistics().getViewCount()));
            result.setLikesCount(parseLong(video.getStatistics().getLikeCount()));
            result.setCommentsCount(parseLong(video.getStatistics().getCommentCount()));
        }

        // 해시태그 추출
        result.setTags(extractHashtags(video.getSnippet().getDescription()));

        // 썸네일 URL 및 크기 정보 추출 (Maxres 우선, 없으면 fallback)
        var thumbnails = video.getSnippet().getThumbnails();
        if (thumbnails.getMaxres() != null) {
            result.setThumbnailUrl(thumbnails.getMaxres().getUrl());
            result.setThumbnailWidth(thumbnails.getMaxres().getWidth());
            result.setThumbnailHeight(thumbnails.getMaxres().getHeight());
        } else if (thumbnails.getHigh() != null) {
            result.setThumbnailUrl(thumbnails.getHigh().getUrl());
            result.setThumbnailWidth(thumbnails.getHigh().getWidth());
            result.setThumbnailHeight(thumbnails.getHigh().getHeight());
        } else if (thumbnails.getMedium() != null) {
            result.setThumbnailUrl(thumbnails.getMedium().getUrl());
            result.setThumbnailWidth(thumbnails.getMedium().getWidth());
            result.setThumbnailHeight(thumbnails.getMedium().getHeight());
        } else if (thumbnails.getDefaultThumbnail() != null) {
            result.setThumbnailUrl(thumbnails.getDefaultThumbnail().getUrl());
            result.setThumbnailWidth(thumbnails.getDefaultThumbnail().getWidth());
            result.setThumbnailHeight(thumbnails.getDefaultThumbnail().getHeight());
        }

        result.setComments(new ArrayList<>());
        return result;
    }

    // 문자열을 Long으로 안전하게 변환하는 유틸리티 메서드
    private static Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // 설명 텍스트에서 해시태그를 추출하는 메서드
    private static List<String> extractHashtags(String description) {
        if (description == null || description.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> tags = new ArrayList<>();
        // #으로 시작하고 공백, 줄바꿈, 탭이 아닌 문자들이 이어지는 패턴 찾기
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([^\\s#]+)");
        java.util.regex.Matcher matcher = pattern.matcher(description);

        while (matcher.find()) {
            String tag = matcher.group(1); // # 제외하고 태그 내용만
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }

        return tags;
    }

    private static Integer parseDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.trim().isEmpty()) {
            return null;
        }

        try {
            java.time.Duration duration = java.time.Duration.parse(isoDuration);
            return (int) duration.getSeconds();
        } catch (Exception e) {
            return null;
        }
    }
}