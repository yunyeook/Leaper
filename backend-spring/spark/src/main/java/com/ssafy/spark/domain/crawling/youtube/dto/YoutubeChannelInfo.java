package com.ssafy.spark.domain.crawling.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YoutubeChannelInfo {
    private String externalAccountId;
    private String accountNickname;
    private Long followersCount;
    private Long postsCount;
    private String profileImageUrl;
}