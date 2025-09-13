package com.ssafy.leaper.domain.content.service;

import com.ssafy.leaper.domain.content.dto.response.ContentDetailResponse;
import com.ssafy.leaper.domain.content.dto.response.ContentListResponse;
import com.ssafy.leaper.domain.content.dto.response.SimilarContentListResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

public interface ContentService {
  ServiceResult<ContentListResponse> getContentsByPlatformAccountId(Long platformAccountId);
  ServiceResult<ContentDetailResponse> getContentById(Long contentId);
  ServiceResult<SimilarContentListResponse> getSimilarContents(Long contentId);


}