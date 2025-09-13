package com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight;

import java.util.List;

public record DailyTrendingContentResponse(
    List<TrendingContentResponse> trendingContents
) {
  public static DailyTrendingContentResponse from(List<TrendingContentResponse> contents) {
    return new DailyTrendingContentResponse(contents);
  }
}
