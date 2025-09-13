package com.ssafy.leaper.domain.content.dto.response;

import com.ssafy.leaper.domain.content.entity.Content;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

public record ContentDetailResponse(
    Long contentId,
    String contentType,
    String title,
    String description,
    String thumbnailUrl,
    String contentUrl,
    Integer durationSeconds,
    LocalDateTime publishedAt,
    List<String> tags,


    BigInteger totalViews,
    BigInteger totalLikes,
    BigInteger totalComments,
    Integer contentRank
) {

  public static ContentDetailResponse from(Content content, Integer contentRank) {
    return new ContentDetailResponse(
        content.getId(),
        content.getContentType().getId(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnail() != null ? content.getThumbnail().getAccessKey() : null,
        content.getContentUrl(),
        content.getDurationSeconds(),
        content.getPublishedAt(),
        content.getTagsJson(),
        content.getTotalViews(),
        content.getTotalLikes(),
        content.getTotalComments(),
        contentRank
    );
  }
}