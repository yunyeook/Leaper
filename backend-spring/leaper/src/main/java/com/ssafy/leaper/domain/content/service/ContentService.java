package com.ssafy.leaper.domain.content.service;

import com.ssafy.leaper.domain.content.dto.ContentListResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

public interface ContentService {
  ServiceResult<ContentListResponse> getContentsByPlatformAccountId(Long platformAccountId);
}