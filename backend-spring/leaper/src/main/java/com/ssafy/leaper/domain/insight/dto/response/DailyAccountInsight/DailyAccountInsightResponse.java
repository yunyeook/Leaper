package com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight;

import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import java.math.BigInteger;
import java.time.LocalDate;

public record DailyAccountInsightResponse(
    Long platformAccountId,
    String platformTypeId,
    BigInteger totalViews,
    Integer totalFollowers,
    Integer totalContents,
    BigInteger totalLikes,
    BigInteger totalComments,
    BigInteger likeScore,
    LocalDate snapshotDate
) {
  public static DailyAccountInsightResponse from(DailyAccountInsight entity) {
    return new DailyAccountInsightResponse(
        entity.getPlatformAccount().getId(),
        entity.getPlatformAccount().getPlatformType().getId(),
        entity.getTotalViews(),
        entity.getTotalFollowers(),
        entity.getTotalContents(),
        entity.getTotalLikes(),
        entity.getTotalComments(),
        entity.getLikeScore(),
        entity.getSnapshotDate()
    );
  }
}
