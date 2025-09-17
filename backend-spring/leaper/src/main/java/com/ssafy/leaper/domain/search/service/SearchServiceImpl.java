package com.ssafy.leaper.domain.search.service;

import com.ssafy.leaper.domain.search.dto.request.InfluencerSearchRequest;
import com.ssafy.leaper.domain.search.dto.response.InfluencerSearchResponse;
import com.ssafy.leaper.domain.search.repository.SearchRepository;
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
public class SearchServiceImpl implements SearchService {

    private final SearchRepository searchRepository;

    @Override
    public ServiceResult<InfluencerSearchResponse> searchInfluencers(InfluencerSearchRequest request) {
        log.info("Searching influencers with criteria: keyword={}, platform={}, category={}, contentType={}, minFollowers={}, maxFollowers={}",
                request.getKeyword(), request.getPlatform(), request.getCategory(),
                request.getContentType(), request.getMinFollowers(), request.getMaxFollowers());

        try {
            List<InfluencerSearchResponse.InfluencerInfo> influencers = searchRepository.searchInfluencers(request);
            InfluencerSearchResponse response = InfluencerSearchResponse.from(influencers);

            log.info("Found {} influencers matching search criteria", influencers.size());
            return ServiceResult.ok(response);

        } catch (Exception e) {
            log.error("Failed to search influencers", e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }
}