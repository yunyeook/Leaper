package com.ssafy.spark.domain.crawling.instagram.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

// CommentRawData.java
@Data
@Builder
public class CommentRawData {
  private Integer contentId;
  private Integer platformAccountId;
  private String contentUrl;
  private Integer commentsCount;
  private List<CommentItem> comments;
  private String crawledAt;

  @Data
  @Builder
  public static class CommentItem {
    private String accountNickname;
    private String externalCommentId;
    private String text;
    private Integer likesCount;
    private String publishedAt;
  }
}