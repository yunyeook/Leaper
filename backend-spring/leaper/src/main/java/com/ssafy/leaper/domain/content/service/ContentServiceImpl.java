package com.ssafy.leaper.domain.content.service;

import com.ssafy.leaper.domain.content.dto.response.ContentDetailResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentListResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentResponse;
import com.ssafy.leaper.domain.content.dto.response.SimilarContentListResponse;
import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.content.repository.ContentRepository;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
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
  private final S3PresignedUrlService s3PresignedUrlService;


  @Override
  public ServiceResult<ContentListResponse> getContentsByPlatformAccountId(Long platformAccountId) {

    if (!platformAccountRepository.existsByIdAndIsDeletedFalse(platformAccountId)) {
      return ServiceResult.fail(ErrorCode.PLATFORM_ACCOUNT_NOT_FOUND);
    }
    List<Content> contents = contentRepository.findByPlatformAccountIdWithContentType(platformAccountId);

    List<ContentResponse> contentDtos = contents.stream()
        .map(content -> ContentResponse.from(content, s3PresignedUrlService))
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

    Integer contentRank = contentRepository.findContentRankByContentId(contentId).orElse(null);

    ContentDetailResponse response = ContentDetailResponse.from(content, contentRank,s3PresignedUrlService);
    return ServiceResult.ok(response);
  }
  @Override
  public ServiceResult<SimilarContentListResponse> getSimilarContents(Long contentId) {
    Content base = contentRepository.findByIdWithDetails(contentId).orElse(null);
    if (base == null) {
      return ServiceResult.fail(ErrorCode.CONTENT_NOT_FOUND);
    }

    String platformTypeId = base.getPlatformType().getId();
    String contentTypeId = base.getContentType().getId();
    Integer baseDuration = base.getDurationSeconds() != null ? base.getDurationSeconds() : 0;

    Long categoryTypeId = contentRepository.findCategoryTypeById(contentId).orElse(null);
    if (categoryTypeId == null) {
      return ServiceResult.fail(ErrorCode.CONTENT_NOT_FOUND);
    }

    List<Content> similars = contentRepository.findSimilarByCategory(
        contentId, platformTypeId, contentTypeId, categoryTypeId, baseDuration);

    List<ContentDetailResponse> responses = similars.stream()
        .map(c -> ContentDetailResponse.from(c, null,s3PresignedUrlService))
        .toList();

    return ServiceResult.ok(SimilarContentListResponse.from(responses));
  }



}