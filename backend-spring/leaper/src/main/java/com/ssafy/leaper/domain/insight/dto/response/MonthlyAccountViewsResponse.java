package com.ssafy.leaper.domain.insight.dto.response;

import java.math.BigInteger;
import java.time.YearMonth;

public record MonthlyAccountViewsResponse(
    Long platformAccountId,
    String platformTypeId,
    BigInteger totalViews,
    YearMonth snapshotDate
) { public static MonthlyAccountViewsResponse of(
    Long platformAccountId,
    String platformTypeId,
    BigInteger totalViews,
    YearMonth snapshotDate
) {
  return new MonthlyAccountViewsResponse(platformAccountId, platformTypeId, totalViews, snapshotDate);
}}