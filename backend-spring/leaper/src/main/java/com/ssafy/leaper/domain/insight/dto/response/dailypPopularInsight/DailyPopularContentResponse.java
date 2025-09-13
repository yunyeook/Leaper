package com.ssafy.leaper.domain.insight.dto.response.dailypPopularInsight;

import java.util.List;

public record DailyPopularContentResponse(
    List<PopularContentResponse> popularContents
) {
  public static DailyPopularContentResponse from(List<PopularContentResponse> contents) {
    return new DailyPopularContentResponse(contents);
  }
}
