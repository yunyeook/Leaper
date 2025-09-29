package com.ssafy.spark.domain.business.platformAccount.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccountCreateRequest {

    private Integer influencerId;
    private String platformTypeId;
    private String externalAccountId;
    private String accountNickname;
    private String accountUrl;
    private Integer accountProfileImageId;
    private Short categoryTypeId;
}