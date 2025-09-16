package com.ssafy.leaper.domain.search.repository;

import com.ssafy.leaper.domain.search.dto.request.InfluencerSearchRequest;
import com.ssafy.leaper.domain.search.dto.response.InfluencerSearchResponse;

import java.util.List;

public interface SearchRepository {
    List<InfluencerSearchResponse.InfluencerInfo> searchInfluencers(InfluencerSearchRequest request);
}