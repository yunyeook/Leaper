// ContentCommentInsightService.java
package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.contentCommentInsight.ContentCommentInsightResponse;
import com.ssafy.leaper.domain.insight.entity.ContentCommentInsight;
import com.ssafy.leaper.domain.insight.repository.ContentCommentInsightRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContentCommentInsightService {

  private final ContentCommentInsightRepository contentCommentInsightRepository;

  /**
   * 특정 콘텐츠의 댓글 인사이트 조회
   */
  public ServiceResult<ContentCommentInsightResponse> getContentCommentInsight(Long contentId) {
    ContentCommentInsight insight = contentCommentInsightRepository
        .findLatestByContentId(contentId)
        .orElse(null);

    if (insight == null) {
      return ServiceResult.fail(ErrorCode.CONTENT_NOT_FOUND);
    }

    ContentCommentInsightResponse response = ContentCommentInsightResponse.from(insight);
    return ServiceResult.ok(response);
  }
}