package com.ssafy.spark.domain.business.platformAccount.dto.response;

import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformAccountResponse {

    private Integer id;
    private Integer influencerId;
    private String platformTypeId;
    private String externalAccountId;
    private String accountNickname;
    private String accountUrl;
    private Integer accountProfileImageId;
    private Short categoryTypeId;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PlatformAccountResponse from(PlatformAccount platformAccount) {
        return PlatformAccountResponse.builder()
                .id(platformAccount.getId())
                .influencerId(platformAccount.getInfluencer() != null ? platformAccount.getInfluencer().getId() : null)
                .platformTypeId(platformAccount.getPlatformType() != null ? platformAccount.getPlatformType().getId() : null)
                .externalAccountId(platformAccount.getExternalAccountId())
                .accountNickname(platformAccount.getAccountNickname())
                .accountUrl(platformAccount.getAccountUrl())
                .accountProfileImageId(platformAccount.getAccountProfileImageId())
                .categoryTypeId(platformAccount.getCategoryType() != null ? platformAccount.getCategoryType().getId() : null)
                .isDeleted(platformAccount.getIsDeleted())
                .deletedAt(platformAccount.getDeletedAt())
                .createdAt(platformAccount.getCreatedAt())
                .updatedAt(platformAccount.getUpdatedAt())
                .build();
    }
}