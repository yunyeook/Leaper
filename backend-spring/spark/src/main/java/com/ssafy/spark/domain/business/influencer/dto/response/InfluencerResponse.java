package com.ssafy.spark.domain.business.influencer.dto.response;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerResponse {

    private Integer id;
    private String providerTypeId;
    private String providerMemberId;
    private String nickname;
    private Boolean gender;
    private LocalDate birthday;
    private String email;
    private Integer profileImageId;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    public static InfluencerResponse from(Influencer influencer) {
        return InfluencerResponse.builder()
                .id(influencer.getId())
                .providerTypeId(influencer.getProviderType() != null ? influencer.getProviderType().getId() : null)
                .providerMemberId(influencer.getProviderMemberId())
                .nickname(influencer.getNickname())
                .gender(influencer.getGender())
                .birthday(influencer.getBirthday())
                .email(influencer.getEmail())
                .profileImageId(influencer.getProfileImageId())
                .bio(influencer.getBio())
                .createdAt(influencer.getCreatedAt())
                .updatedAt(influencer.getUpdatedAt())
                .isDeleted(influencer.getIsDeleted())
                .deletedAt(influencer.getDeletedAt())
                .build();
    }
}