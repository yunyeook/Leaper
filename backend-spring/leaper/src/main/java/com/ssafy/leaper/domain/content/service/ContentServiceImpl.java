package com.ssafy.leaper.domain.content.service;

import com.ssafy.leaper.domain.content.dto.response.ContentDetailResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentListResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentResponse;
import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.content.repository.ContentRepository;
import com.ssafy.leaper.domain.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

  private final ContentRepository contentRepository;
  private final PlatformAccountRepository platformAccountRepository;

  @Override
  public ServiceResult<ContentListResponse> getContentsByPlatformAccountId(Long platformAccountId) {

    if (!platformAccountRepository.existsByIdAndIsDeletedFalse(platformAccountId)) {
      return ServiceResult.fail(ErrorCode.PLATFORM_ACCOUNT_NOT_FOUND);
    }
    List<Content> contents = contentRepository.findByPlatformAccountIdWithContentType(platformAccountId);

    List<ContentResponse> contentDtos = contents.stream()
        .map(ContentResponse::from)
        .toList();

    ContentListResponse response = ContentListResponse.from(contentDtos);
    return ServiceResult.ok(response);
  }

  @Override
  public ServiceResult<ContentDetailResponse> getContentById(Long contentId) {

    Content content = contentRepository.findByIdWithDetails(contentId).orElse(null);
    if (content == null) {
      return ServiceResult.fail(ErrorCode.CONTENT_NOT_FOUND);
    }

    Integer contentRank = contentRepository.findContentRankByContentId(contentId)
        .filter(rank -> rank <= 50)
        .orElse(null);

    ContentDetailResponse response = ContentDetailResponse.from(content, contentRank);
    return ServiceResult.ok(response);
  }
}