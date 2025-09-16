package com.ssafy.leaper.domain.search.service;

import com.ssafy.leaper.domain.search.dto.request.InfluencerSearchRequest;
import com.ssafy.leaper.domain.search.dto.response.InfluencerSearchResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;

public interface SearchService {
    ServiceResult<InfluencerSearchResponse> searchInfluencers(InfluencerSearchRequest request);
}