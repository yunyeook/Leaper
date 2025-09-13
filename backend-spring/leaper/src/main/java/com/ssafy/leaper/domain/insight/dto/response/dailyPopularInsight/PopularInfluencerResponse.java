package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import com.ssafy.leaper.domain.insight.entity.DailyPopularInfluencer;

public record PopularInfluencerResponse(
    Long influencerId,
    Integer influencerRank,
    String accountNickname,
    Integer totalFollowers
) {
  public static PopularInfluencerResponse of(
      DailyPopularInfluencer dpi,
      Integer totalFollowers
  ) {
    return new PopularInfluencerResponse(
        dpi.getInfluencer().getId(),
        dpi.getInfluencerRank(),
        dpi.getInfluencer().getNickname(),
        totalFollowers
    );
  }
}
