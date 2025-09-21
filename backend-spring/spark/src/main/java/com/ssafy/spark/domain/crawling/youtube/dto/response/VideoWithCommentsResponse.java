package com.ssafy.spark.domain.crawling.youtube.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.spark.domain.crawling.youtube.dto.YoutubeVideoWithComments;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoWithCommentsResponse {
    private String contentId;
    private String contentUrl;
    private String contentType;
    private Long commentsCount;
    private List<CommentInfo> comments;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommentInfo {
        private String accountNickname;
        private String externalCommentId;
        private String text;
        private Long likesCount;
        private String publishedAt;
        private String crawledAt;
    }

    public static VideoWithCommentsResponse fromYouTubeVideoWithComments(YoutubeVideoWithComments video) {
        VideoWithCommentsResponse response = new VideoWithCommentsResponse();

        // TODO : DB상 pk로 변경
        response.setContentId(video.getVideoId()); // DB가 없으니 videoId를 contentId로 사용
        response.setContentUrl("https://www.youtube.com/watch?v=" + video.getVideoId());
        response.setCommentsCount(video.getCommentsCount());

        List<CommentInfo> commentInfos = new java.util.ArrayList<>();
        if (video.getComments() != null) {
            for (YoutubeVideoWithComments.CommentInfo comment : video.getComments()) {
                CommentInfo commentInfo = new CommentInfo();
                commentInfo.setAccountNickname(comment.getAuthorName());
                commentInfo.setExternalCommentId(comment.getCommentId());
                commentInfo.setText(comment.getCommentText());
                commentInfo.setLikesCount(comment.getLikeCount() != null ? comment.getLikeCount().longValue() : 0L);
                commentInfo.setPublishedAt(comment.getPublishedAt());
                commentInfo.setCrawledAt(LocalDateTime.now().toString());
                commentInfos.add(commentInfo);
            }
        }
        response.setComments(commentInfos);

        return response;
    }
}