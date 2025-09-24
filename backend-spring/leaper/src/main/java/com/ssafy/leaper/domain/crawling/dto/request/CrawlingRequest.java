package com.ssafy.leaper.domain.crawling.dto.request;

import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;


public record CrawlingRequest (
   Integer platformAccountId,
   String platformTypeId,       // ex) instagram, youtube 소문자로
   String accountUrl,
   String accountNickname,
   String externalAccountId,
   Short categoryTypeId
){
public static CrawlingRequest from(PlatformAccount platformAccount ){
   return new CrawlingRequest(
       platformAccount.getId(),
       platformAccount.getPlatformType().getId().toLowerCase(), // instagram, youtube 등 소문자로 요청
       platformAccount.getAccountUrl(),
       platformAccount.getAccountNickname(),
       platformAccount.getExternalAccountId(),
       platformAccount.getCategoryType().getId()
  );
}
}
