package com.ssafy.leaper.domain.insight.dto.response;

import java.util.List;

public record InfluencerViewsResponse(
    List<DailyAccountViewsResponse> dailyAccountViews,
    List<MonthlyAccountViewsResponse> monthlyAccountViews
) {
  public static InfluencerViewsResponse of(
      List<DailyAccountViewsResponse> daily,
      List<MonthlyAccountViewsResponse> monthly
  ) {
    return new InfluencerViewsResponse(daily, monthly);
  }
}
