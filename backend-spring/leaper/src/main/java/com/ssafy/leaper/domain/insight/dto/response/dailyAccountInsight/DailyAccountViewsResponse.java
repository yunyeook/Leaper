package com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight;

import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import java.math.BigInteger;
import java.time.LocalDate;

public record DailyAccountViewsResponse(
    Long platformAccountId,
    String platformTypeId,
    BigInteger totalViews,
    LocalDate snapshotDate
) {
  public static DailyAccountViewsResponse from(DailyAccountInsight dailyAccountInsight){
    return new DailyAccountViewsResponse(
        dailyAccountInsight.getPlatformAccount().getId(),
        dailyAccountInsight.getPlatformAccount().getPlatformType().getId(),
        dailyAccountInsight.getTotalViews(),
        dailyAccountInsight.getSnapshotDate()
    );
  }
}


