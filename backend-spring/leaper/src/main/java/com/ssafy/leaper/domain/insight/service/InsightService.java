package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.AccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.InfluencerViewsResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

public interface InsightService {
   ServiceResult<AccountInsightResponse> getAccountInsights(Long influencerId) ;
   ServiceResult<AccountInsightResponse> getPlatformAccountInsights(Long platformAccountId);
   ServiceResult<InfluencerViewsResponse> getInfluencerViews(Long influencerId);
   ServiceResult<InfluencerViewsResponse> getPlatformAccountViews(Long platformAccountId);
}
