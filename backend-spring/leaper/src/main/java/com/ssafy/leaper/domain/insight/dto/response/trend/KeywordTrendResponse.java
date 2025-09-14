package com.ssafy.leaper.domain.insight.dto.response.trend;

import java.util.List;

public record KeywordTrendResponse(
    List<String> keywords
) {
  public static KeywordTrendResponse from(List<String> keywords) {
    return new KeywordTrendResponse(keywords);
  }


}