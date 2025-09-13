package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.content.dto.response.ContentListResponse;
import com.ssafy.leaper.domain.insight.dto.response.AccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.InfluencerViewsResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

public interface InsightService {
   ServiceResult<AccountInsightResponse> getAccountInsights(Long influencerId) ;
   ServiceResult<InfluencerViewsResponse> getInfluencerViews(Long influencerId);

   }
