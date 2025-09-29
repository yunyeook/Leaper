package com.ssafy.spark.domain.crawling.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentThread {
    private String kind;
    private String etag;
    private String id;
    private Snippet snippet;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String videoId;
        private TopLevelComment topLevelComment;
        private Boolean canReply;
        private Integer totalReplyCount;
        private Boolean isPublic;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TopLevelComment {
            private String kind;
            private String etag;
            private String id;
            private CommentSnippet snippet;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class CommentSnippet {
                private String videoId;
                private String textDisplay;
                private String textOriginal;
                private String authorDisplayName;
                private String authorProfileImageUrl;
                private String authorChannelUrl;
                private AuthorChannelId authorChannelId;
                private Boolean canRate;
                private String viewerRating;
                private Integer likeCount;
                private String publishedAt;
                private String updatedAt;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class AuthorChannelId {
                    private String value;
                }
            }
        }
    }
}
