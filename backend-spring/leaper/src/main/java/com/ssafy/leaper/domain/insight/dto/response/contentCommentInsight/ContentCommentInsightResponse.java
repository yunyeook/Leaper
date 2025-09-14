package com.ssafy.leaper.domain.insight.dto.response.contentCommentInsight;

import com.ssafy.leaper.domain.insight.entity.ContentCommentInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContentCommentInsightResponse(
    Long contentId,
    String contentTitle,
    List<String> positiveComments,
    List<String> negativeComments,
    BigDecimal likeScore,
    String summaryText,
    List<String> keywords,
    String modelVersion,
    LocalDate snapshotDate
) {
  public static ContentCommentInsightResponse from(ContentCommentInsight entity) {
    return new ContentCommentInsightResponse(
        entity.getContent().getId(),
        entity.getContent().getTitle(),
        entity.getPositiveComments(),
        entity.getNegativeComments(),
        entity.getLikeScore(),
        entity.getSummaryText(),
        entity.getKeywords(),
        entity.getModelVersion(),
        entity.getSnapshotDate()
    );
  }
}