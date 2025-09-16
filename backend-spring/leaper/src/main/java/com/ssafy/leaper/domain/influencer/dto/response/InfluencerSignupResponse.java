package com.ssafy.leaper.domain.influencer.dto.response;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerSignupResponse {

    private Integer influencerId;
    private String accessToken;

    public static InfluencerSignupResponse from(Influencer influencer, String accessToken) {
        return InfluencerSignupResponse.builder()
                .influencerId(influencer.getId())
                .accessToken(accessToken)
                .build();
    }
}