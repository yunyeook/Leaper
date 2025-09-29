package com.ssafy.spark.domain.crawling.youtube.dto.response.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.spark.domain.crawling.youtube.dto.CommentThread;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawYoutubeCommentResponse {
    private String kind;
    private String etag;
    private String nextPageToken;
    private PageInfo pageInfo;
    private List<CommentThread> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        private Integer totalResults;
        private Integer resultsPerPage;
    }
}
