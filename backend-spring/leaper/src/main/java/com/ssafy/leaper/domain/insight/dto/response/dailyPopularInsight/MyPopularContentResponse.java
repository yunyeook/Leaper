package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.insight.entity.DailyMyPopularContent;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyPopularContentResponse(
    Long contentId,
    String title,
    String thumbnailUrl,
    BigInteger viewCount,
    BigInteger likeCount,
    Integer durationSeconds,
    LocalDateTime publishedAt,
    LocalDate snapshotDate
) {
  public static MyPopularContentResponse of(DailyMyPopularContent dmpc) {
    Content c = dmpc.getContent();
    return new MyPopularContentResponse(
        c.getId(),
        c.getTitle(),
        c.getThumbnail() != null ? c.getThumbnail().getAccessKey() : null,
        c.getTotalViews(),
        c.getTotalLikes(),
        c.getDurationSeconds(),
        c.getPublishedAt(),
        dmpc.getSnapshotDate()
    );
  }
}
