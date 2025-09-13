package com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight;

import java.util.List;

public record DailyTrendingInfluencerResponse(
    List<TrendingInfluencerResponse> popularInfluencers
) {
  public static DailyTrendingInfluencerResponse from(List<TrendingInfluencerResponse> influencers) {
    return new DailyTrendingInfluencerResponse(influencers);
  }
}
