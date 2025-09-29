package com.ssafy.spark.domain.crawling.instagram.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContentRawData {
  private String accountNickname;
  private String externalContentId;
  private String platformType;
  private String contentType;
  private String title;
  private String description;
  private Integer durationSeconds;
  private String contentUrl;
  private String publishedAt;
  private List<String> tags;
  private Long viewsCount;
  private Long likesCount;
  private Long commentsCount;
  private ThumbnailInfo thumbnailInfo;

  @Data
  @Builder
  public static class ThumbnailInfo {
    private String accessKey;
    private String contentType;
  }
}