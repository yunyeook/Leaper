package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import java.util.List;

public record DailyPopularInfluencerResponse(
    List<PopularInfluencerResponse> popularInfluencers
) {
  public static DailyPopularInfluencerResponse from(List<PopularInfluencerResponse> influencers) {
    return new DailyPopularInfluencerResponse(influencers);
  }
}
