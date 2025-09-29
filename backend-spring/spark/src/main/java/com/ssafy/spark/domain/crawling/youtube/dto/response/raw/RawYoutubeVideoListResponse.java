package com.ssafy.spark.domain.crawling.youtube.dto.response.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssafy.spark.domain.crawling.youtube.dto.YoutubeVideo;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawYoutubeVideoListResponse {
    private String kind;
    private String etag;
    private String nextPageToken;
    private String prevPageToken;
    private PageInfo pageInfo;
    private List<YoutubeVideo> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        private int totalResults;
        private int resultsPerPage;
    }
}
