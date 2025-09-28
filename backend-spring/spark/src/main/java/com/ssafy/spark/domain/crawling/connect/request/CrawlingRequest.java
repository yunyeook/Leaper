package com.ssafy.spark.domain.crawling.connect.request;

import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlingRequest {
  private Integer influencerId;
  private Integer platformAccountId;
  private String platformTypeId;      // instagram, youtube 등
  private String accountUrl;
  private String accountNickname;
  private String externalAccountId;
  private Integer categoryTypeId;
  public static CrawlingRequest from(PlatformAccount platformAccount){
    return CrawlingRequest.builder()
    .influencerId(platformAccount.getInfluencer().getId())
    .platformAccountId(platformAccount.getId())
    .platformTypeId(platformAccount.getPlatformType().getId())
    .accountUrl(platformAccount.getAccountUrl())
    .accountNickname(platformAccount.getAccountNickname())
    .externalAccountId(platformAccount.getExternalAccountId())
    .categoryTypeId(platformAccount.getCategoryType().getId().intValue()) // 수정된 부분
    .build(); // 수정된 부분
  }
}