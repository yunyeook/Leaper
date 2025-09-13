package com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight;

import com.ssafy.leaper.domain.insight.entity.DailyTypeInsight;
import java.math.BigInteger;
import java.time.LocalDate;

public record DailyTypeInsightResponse(
    Long platformAccountId,
    String platformTypeId,
    String contentTypeId,
    Integer todayViews,
    Integer todayContents,
    Integer todayLikes,
    BigInteger monthViews,
    Integer monthContents,
    BigInteger monthLikes,
    BigInteger totalViews,
    Integer totalContents,
    BigInteger totalLikes,
    LocalDate snapshotDate
) {
  public static DailyTypeInsightResponse from(DailyTypeInsight entity) {
    return new DailyTypeInsightResponse(
        entity.getPlatformAccount().getId(),
        entity.getPlatformAccount().getPlatformType().getId(),
        entity.getContentType().getId(),
        entity.getTodayViews(),
        entity.getTodayContents(),
        entity.getTodayLikes(),
        entity.getMonthViews(),
        entity.getMonthContents(),
        entity.getMonthLikes(),
        entity.getTotalViews(),
        entity.getTotalContents(),
        entity.getTotalLikes(),
        entity.getSnapshotDate()
    );
  }
}
