package com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.insight.entity.DailyMyPopularContent;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyPopularContentResponse(
    Integer contentId,
    String title,
    String thumbnailUrl,
    BigInteger viewCount,
    BigInteger likeCount,
    Integer durationSeconds,
    LocalDateTime publishedAt,
    LocalDate snapshotDate
) {
  public static MyPopularContentResponse of(DailyMyPopularContent dmpc, S3PresignedUrlService s3PresignedUrlService) {
    Content content = dmpc.getContent();
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




    return new MyPopularContentResponse(
        content.getId(),
        content.getTitle(),
        thumbnailUrl,
        content.getTotalViews(),
        content.getTotalLikes(),
        content.getDurationSeconds(),
        content.getPublishedAt(),
        dmpc.getSnapshotDate()
    );
  }
}
