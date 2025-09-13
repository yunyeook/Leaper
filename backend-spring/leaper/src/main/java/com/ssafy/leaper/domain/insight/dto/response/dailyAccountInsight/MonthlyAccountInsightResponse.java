package com.ssafy.leaper.domain.insight.dto.response.dailyAccountInsight;

import java.math.BigInteger;
import java.time.YearMonth;

public record MonthlyAccountInsightResponse(
    Long platformAccountId,
    String platformTypeId,
    BigInteger totalViews,
    Integer totalFollowers,
    Integer totalContents,
    BigInteger totalLikes,
    BigInteger totalComments,
    YearMonth snapshotDate
) {
  public static MonthlyAccountInsightResponse of(
      Long platformAccountId,
      String platformTypeId,
      BigInteger totalViews,
      Integer totalFollowers,
      Integer totalContents,
      BigInteger totalLikes,
      BigInteger totalComments,
      YearMonth snapshotDate
  ) {
    return new MonthlyAccountInsightResponse(
        platformAccountId,
        platformTypeId,
        totalViews,
        totalFollowers,
        totalContents,
        totalLikes,
        totalComments,
        snapshotDate
    );
  }
}
