package com.ssafy.leaper.domain.influencer.dto.response;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class InfluencerPublicProfileResponse {

    private String nickname;
    private String gender;
    private LocalDate birthday;
    private String email;
    private String influencerProfileImageUrl;
    private String bio;

    public static InfluencerPublicProfileResponse from(Influencer influencer, String profileImageUrl) {
        return InfluencerPublicProfileResponse.builder()
                .nickname(influencer.getNickname())
                .gender(influencer.getGender() ? "FEMALE" : "MALE")
                .birthday(influencer.getBirthday())
                .email(influencer.getEmail())
                .influencerProfileImageUrl(profileImageUrl)
                .bio(influencer.getBio())
                .build();
    }
}