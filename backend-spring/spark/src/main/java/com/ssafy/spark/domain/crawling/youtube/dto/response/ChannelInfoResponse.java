package com.ssafy.spark.domain.crawling.youtube.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelInfoResponse {
    private String externalAccountId;
    private String accountNickname;
    private String categoryName;
    private String accountUrl;
    private Long followersCount;
    private Long postsCount;
    private String crawledAt;
    private ProfileImageInfo profileImageInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileImageInfo {
        private String accessKey;
        private String contentType;
    }
}