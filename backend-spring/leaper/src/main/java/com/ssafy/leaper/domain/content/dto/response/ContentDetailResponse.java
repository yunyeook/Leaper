package com.ssafy.leaper.domain.content.dto.response;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

public record ContentDetailResponse(
    Integer contentId,
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

  public static ContentDetailResponse from(Content content,
      Integer contentRank,
      S3PresignedUrlService s3PresignedUrlService) {
    // presigned URL 생성
    String thumbnailUrl = null;
    if (content.getThumbnail() != null) {
      try {
        thumbnailUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
            content.getThumbnail().getId()
        );
      } catch (Exception e) {
        // 실패하면 null로 두고 로그만 출력
        System.out.println("썸네일 presigned URL 생성 실패: " + e.getMessage());
      }
    }

    return new ContentDetailResponse(
        content.getId(),
        content.getContentType().getId(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
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
