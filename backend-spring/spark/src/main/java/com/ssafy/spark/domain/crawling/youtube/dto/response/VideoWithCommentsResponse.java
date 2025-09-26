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
}