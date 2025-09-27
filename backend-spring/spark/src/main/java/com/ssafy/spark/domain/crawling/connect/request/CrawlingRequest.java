package com.ssafy.spark.domain.crawling.connect.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrawlingRequest {
  private Integer influencerId;
  private Integer platformAccountId;
  private String platformTypeId;      // instagram, youtube 등
  private String accountUrl;
  private String accountNickname;
  private String externalAccountId;
  private Integer categoryTypeId;
}