package com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight;

import java.util.List;

public record TypeInsightResponse(
    List<DailyTypeInsightResponse> dailyTypeInsights,
    List<MonthlyTypeInsightResponse> monthlyTypeInsights
) {
  public static TypeInsightResponse of(
      List<DailyTypeInsightResponse> daily,
      List<MonthlyTypeInsightResponse> monthly
  ) {
    return new TypeInsightResponse(daily, monthly);
  }
}
