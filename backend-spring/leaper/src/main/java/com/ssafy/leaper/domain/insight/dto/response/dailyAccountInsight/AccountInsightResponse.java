package com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight;

import java.util.List;

public record AccountInsightResponse(
    List<DailyAccountInsightResponse> dailyAccountInsights,
    List<MonthlyAccountInsightResponse> monthlyAccountInsights
) {
  public static AccountInsightResponse of(
      List<DailyAccountInsightResponse> daily,
      List<MonthlyAccountInsightResponse> monthly
  ) {
    return new AccountInsightResponse(daily, monthly);
  }
}
