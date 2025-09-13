package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import java.util.List;

public record DailyMyPopularContentResponse(
    List<MyPopularContentResponse> popularContents
) {
  public static DailyMyPopularContentResponse from(List<MyPopularContentResponse> contents) {
    return new DailyMyPopularContentResponse(contents);
  }
}
