package com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight;

import com.ssafy.leaper.domain.insight.entity.DailyTrendingInfluencer;

public record TrendingInfluencerResponse(
    Long influencerId,
    Integer influencerRank,
    String accountNickname,
    Integer totalFollowers
) {
  public static TrendingInfluencerResponse of(
      DailyTrendingInfluencer dpi,
      Integer totalFollowers
  ) {
    return new TrendingInfluencerResponse(
        dpi.getInfluencer().getId(),
        dpi.getInfluencerRank(),
        dpi.getInfluencer().getNickname(),
        totalFollowers
    );
  }
}
