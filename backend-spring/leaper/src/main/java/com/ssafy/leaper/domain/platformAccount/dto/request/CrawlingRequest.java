package com.ssafy.leaper.domain.platformAccount.dto.request;

import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
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
  public static CrawlingRequest from(PlatformAccount platformAccount) {
    return new CrawlingRequest(
        platformAccount.getInfluencer().getId(),
        platformAccount.getId(),
        platformAccount.getPlatformType().getId().toLowerCase(), // INSTAGRAM 등 대문자를 소문자로 변환
        platformAccount.getAccountUrl(),
        platformAccount.getAccountNickname(),
        platformAccount.getExternalAccountId(),
        platformAccount.getCategoryType() != null ?
            platformAccount.getCategoryType().getId().intValue() : null
    );
  }
}