package com.ssafy.leaper.domain.influencer.dto.response;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerSignupResponse {

    private Long influencerId;

    public static InfluencerSignupResponse from(Influencer influencer) {
        return InfluencerSignupResponse.builder()
                .influencerId(influencer.getInfluencerId())
                .build();
    }
}