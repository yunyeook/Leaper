package com.ssafy.spark.domain.crawling.youtube.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoInfoResponse {
    private String accountNickname;
    private String externalContentId;
    private String platformType;
    private String contentType;
    private String title;
    private String description;
    private Integer durationSeconds;
    private String contentUrl;
    private String publishedAt;
    private List<String> tags;
    private Long viewsCount;
    private Long likesCount;
    private Long commentsCount;
    private ThumbnailInfo thumbnailInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThumbnailInfo {
        private String accessKey;
        private String contentType;
    }
}