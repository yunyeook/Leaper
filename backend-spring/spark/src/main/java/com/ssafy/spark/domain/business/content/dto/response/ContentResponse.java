package com.ssafy.spark.domain.business.content.dto.response;

import com.ssafy.spark.domain.business.content.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private Integer id;
    private Integer platformAccountId;
    private String platformTypeId;
    private String contentTypeId;
    private String title;
    private String description;
    private Integer durationSeconds;
    private Integer thumbnailId;
    private String contentUrl;
    private LocalDateTime publishedAt;
    private List<String> tagsJson;
    private BigInteger totalViews;
    private BigInteger totalLikes;
    private BigInteger totalComments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate snapshotDate;

    public static ContentResponse from(Content content) {
        return ContentResponse.builder()
                .id(content.getId())
                .platformAccountId(content.getPlatformAccount() != null ? content.getPlatformAccount().getId() : null)
                .platformTypeId(content.getPlatformType() != null ? content.getPlatformType().getId() : null)
                .contentTypeId(content.getContentType() != null ? content.getContentType().getId() : null)
                .title(content.getTitle())
                .description(content.getDescription())
                .durationSeconds(content.getDurationSeconds())
                .thumbnailId(content.getThumbnailId())
                .contentUrl(content.getContentUrl())
                .publishedAt(content.getPublishedAt())
                .tagsJson(content.getTagsJson())
                .totalViews(content.getTotalViews())
                .totalLikes(content.getTotalLikes())
                .totalComments(content.getTotalComments())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .snapshotDate(content.getSnapshotDate())
                .build();
    }
}