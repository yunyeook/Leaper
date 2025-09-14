package com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight;

import java.math.BigInteger;
import java.time.YearMonth;

public record MonthlyTypeInsightResponse(
        Integer platformAccountId,
        String platformTypeId,
        String contentTypeId,
        BigInteger monthViews,
        Integer monthContents,
        BigInteger monthLikes,
        BigInteger totalViews,
        Integer totalContents,
        BigInteger totalLikes,
        YearMonth snapshotDate
) {
  public static MonthlyTypeInsightResponse of(
          Integer platformAccountId,
          String platformTypeId,
          String contentTypeId,
          BigInteger monthViews,
          Integer monthContents,
          BigInteger monthLikes,
          BigInteger totalViews,
          Integer totalContents,
          BigInteger totalLikes,
          YearMonth snapshotDate
  ) {
    return new MonthlyTypeInsightResponse(
        platformAccountId,
        platformTypeId,
        contentTypeId,
        monthViews,
        monthContents,
        monthLikes,
        totalViews,
        totalContents,
        totalLikes,
        snapshotDate
    );
  }
}
