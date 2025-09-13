package com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight;

import com.ssafy.leaper.domain.insight.entity.DailyTrendingContent;

public record TrendingContentResponse(
    Long contentId,
    Integer contentRank,
    String accountNickname,
    String title,
    String contentUrl
) {
  public static TrendingContentResponse from(DailyTrendingContent entity) {
    return new TrendingContentResponse(
        entity.getContent().getId(),
        entity.getContentRank(),
        entity.getContent().getPlatformAccount().getAccountNickname(),
        entity.getContent().getTitle(),
        entity.getContent().getContentUrl()
    );
  }
}
