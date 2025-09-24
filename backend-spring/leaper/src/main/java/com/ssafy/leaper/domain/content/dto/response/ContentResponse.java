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
                String accessKey = content.getThumbnail().getAccessKey();

                if (accessKey != null && accessKey.startsWith("raw_data")) {
                    // raw_data로 시작하면 presigned URL 생성
                    thumbnailUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                        content.getThumbnail().getId()
                    );
                } else {
                    // raw_data로 시작하지 않으면 accessKey 그대로 사용
                    thumbnailUrl = accessKey;
                }
            } catch (Exception e) {
                // 실패하면 null로 두고 로그만 출력
                System.out.println("썸네일 URL 처리 실패: " + e.getMessage());
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
