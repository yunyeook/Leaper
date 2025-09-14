package com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight;

import java.math.BigInteger;
import java.time.YearMonth;

public record MonthlyAccountViewsResponse(
        Integer platformAccountId,
        String platformTypeId,
        BigInteger totalViews,
        YearMonth snapshotDate
) { public static MonthlyAccountViewsResponse of(
        Integer platformAccountId,
        String platformTypeId,
        BigInteger totalViews,
        YearMonth snapshotDate
) {
  return new MonthlyAccountViewsResponse(platformAccountId, platformTypeId, totalViews, snapshotDate);
}}