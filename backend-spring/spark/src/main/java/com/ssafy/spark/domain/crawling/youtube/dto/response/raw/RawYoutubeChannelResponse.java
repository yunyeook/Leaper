package com.ssafy.spark.domain.crawling.youtube.dto.response.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawYoutubeChannelResponse {
    private String kind;
    private String etag;
    private PageInfo pageInfo;
    private List<YouTubeChannel> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        private Integer totalResults;
        private Integer resultsPerPage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YouTubeChannel {
        private String kind;
        private String etag;
        private String id;
        private Snippet snippet;
        private Statistics statistics;
        private ContentDetails contentDetails;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Snippet {
            private String title;
            private String description;
            private String customUrl;
            private String publishedAt;
            private String defaultLanguage;
            private String country;
            private String handle;
            private Thumbnails thumbnails;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Thumbnails {
                private Thumbnail default_;
                private Thumbnail medium;
                private Thumbnail high;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Thumbnail {
                    private String url;
                    private Integer width;
                    private Integer height;
                }

                // Jackson이 "default" 키워드를 처리할 수 있도록 별칭 메서드 추가
                @com.fasterxml.jackson.annotation.JsonProperty("default")
                public Thumbnail getDefault() {
                    return default_;
                }

                @com.fasterxml.jackson.annotation.JsonProperty("default")
                public void setDefault(Thumbnail thumbnail) {
                    this.default_ = thumbnail;
                }
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Statistics {
            private String viewCount;
            private String subscriberCount;
            private String hiddenSubscriberCount;
            private String videoCount;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ContentDetails {
            private RelatedPlaylists relatedPlaylists;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class RelatedPlaylists {
                private String uploads;
            }
        }
    }
}