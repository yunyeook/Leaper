
package com.ssafy.spark.domain.crawling.instagram.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Instagram 댓글 수집 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CommentRequest {

  /**
   * 댓글을 수집할 Instagram 게시물 URL 목록
   * 예: ["https://www.instagram.com/p/DCZlEDqy2to", "https://www.instagram.com/reel/DDIJAfeyemG"]
   */
  private List<String> directUrls;

  /**
   * 수집할 댓글 개수 (기본값: 15)
   */
  private int resultsLimit = 15;

  /**
   * 대댓글(중첩 댓글) 포함 여부 (기본값: false)
   */
  private boolean includeNestedComments = false;

  /**
   * 최신 댓글부터 수집 여부 (기본값: false)
   */
  private boolean isNewestComments = false;
}