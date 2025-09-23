package com.ssafy.leaper.domain.content.dto.response;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import java.time.LocalDateTime;
import java.util.List;

public record ContentResponse(
    Integer contentId,
    String contentType,
    String title,
    String description,
    String thumbnailUrl,
    String contentUrl,
    Integer durationSeconds,
    LocalDateTime publishedAt,
    List<String> tags
) {
    public static ContentResponse from(Content content, S3PresignedUrlService s3PresignedUrlService) {
        // presigned URL 생성
        String thumbnailUrl = null;
        if (content.getThumbnail() != null) {
            try {
                thumbnailUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                    content.getThumbnail().getId()
                );
            } catch (Exception e) {
                // 로깅만 처리
                System.out.println("썸네일 presigned URL 생성 실패: " + e.getMessage());
            }
        }

        return new ContentResponse(
            content.getId(),
            content.getContentType().getId(),
            content.getTitle(),
            content.getDescription(),
            thumbnailUrl,
            content.getContentUrl(),
            content.getDurationSeconds(),
            content.getPublishedAt(),
            content.getTagsJson()
        );
    }
}
