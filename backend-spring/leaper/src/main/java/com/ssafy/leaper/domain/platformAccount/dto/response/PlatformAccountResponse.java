package com.ssafy.leaper.domain.platformAccount.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlatformAccountResponse {

    private Integer platformAccountId;
    private String platformTypeId;
    private String externalAccountId;
    private String accountNickname;
    private String accountUrl;
    private String accountProfileImageUrl;
    private Short categoryTypeId;

    public static PlatformAccountResponse of(
            Integer platformAccountId,
            String platformTypeId,
            String externalAccountId,
            String accountNickname,
            String accountUrl,
            String accountProfileImageUrl,
            Short categoryTypeId) {

        return PlatformAccountResponse.builder()
                .platformAccountId(platformAccountId)
                .platformTypeId(platformTypeId)
                .externalAccountId(externalAccountId)
                .accountNickname(accountNickname)
                .accountUrl(accountUrl)
                .accountProfileImageUrl(accountProfileImageUrl)
                .categoryTypeId(categoryTypeId)
                .build();
    }
}