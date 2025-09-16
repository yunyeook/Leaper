package com.ssafy.leaper.domain.influencer.dto.response;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class InfluencerUpdateResponse {

    private String nickname;
    private String gender;
    private LocalDate birthday;
    private String email;
    private String influencerProfileImageUrl;
    private String bio;

    public static InfluencerUpdateResponse from(Influencer influencer, String profileImageUrl) {
        return InfluencerUpdateResponse.builder()
                .nickname(influencer.getNickname())
                .gender(influencer.getGender() ? "FEMALE" : "MALE")
                .birthday(influencer.getBirthday())
                .email(influencer.getEmail())
                .influencerProfileImageUrl(profileImageUrl)
                .bio(influencer.getBio())
                .build();
    }
}