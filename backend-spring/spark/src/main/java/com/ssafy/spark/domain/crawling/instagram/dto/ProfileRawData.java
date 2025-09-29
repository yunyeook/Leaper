package com.ssafy.spark.domain.crawling.instagram.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileRawData {
  private String externalAccountId;
  private String accountNickname;
  private String categoryName;
  private String accountUrl;
  private Integer followersCount;
  private Integer postsCount;
  private String crawledAt;
}