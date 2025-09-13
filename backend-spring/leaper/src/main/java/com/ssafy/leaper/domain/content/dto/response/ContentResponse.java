package com.ssafy.leaper.domain.content.dto.response;

import com.ssafy.leaper.domain.content.entity.Content;

import java.time.LocalDateTime;
import java.util.List;

public record ContentResponse(
    Long contentId,
    String contentType,
    String title,
    String description,
    String thumbnailUrl,
    String contentUrl,
    Integer durationSeconds,
    LocalDateTime publishedAt,
    List<String> tags
) {

    public static ContentResponse from(Content content) {

        return new ContentResponse(
            content.getId(),
            content.getContentType().getId(),
            content.getTitle(),
            content.getDescription(),
            content.getThumbnail() != null ? content.getThumbnail().getAccessKey() : null,
            content.getContentUrl(),
            content.getDurationSeconds(),
            content.getPublishedAt(),
            content.getTagsJson()
        );
    }
}