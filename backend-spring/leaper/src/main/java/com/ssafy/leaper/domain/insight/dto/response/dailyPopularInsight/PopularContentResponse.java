package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import com.ssafy.leaper.domain.insight.entity.DailyPopularContent;

public record PopularContentResponse(
    Integer contentId,
    Integer contentRank,
    String accountNickname,
    String title,
    String contentUrl
) {
  public static PopularContentResponse from(DailyPopularContent entity) {
    return new PopularContentResponse(
        entity.getContent().getId(),
        entity.getContentRank(),
        entity.getContent().getPlatformAccount().getAccountNickname(),
        entity.getContent().getTitle(),
        entity.getContent().getContentUrl()
    );
  }
}
