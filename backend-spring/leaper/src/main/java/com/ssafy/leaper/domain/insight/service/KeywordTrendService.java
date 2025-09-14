package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.trend.KeywordTrendResponse;
import com.ssafy.leaper.domain.insight.entity.DailyKeywordTrend;
import com.ssafy.leaper.domain.insight.entity.GoogleKeywordTrend;
import com.ssafy.leaper.domain.insight.repository.DailyKeywordTrendRepository;
import com.ssafy.leaper.domain.insight.repository.GoogleKeywordTrendRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordTrendService {

  private final DailyKeywordTrendRepository dailyKeywordTrendRepository;
  private final GoogleKeywordTrendRepository googleKeywordTrendRepository;

  public ServiceResult<KeywordTrendResponse> getDailyKeywordTrendLatestSnapshot(String platformTypeId) {
    return ServiceResult.ok(
        dailyKeywordTrendRepository.findFirstByPlatformTypeIdOrderBySnapshotDateDescCreatedAtDesc(platformTypeId)
            .map(DailyKeywordTrend::getKeywords)
            .map(KeywordTrendResponse::from)
            .orElse(null)
    );
  }

  public ServiceResult<KeywordTrendResponse> getGoogleKeywordTrendLatestSnapshot() {
    return ServiceResult.ok(
        googleKeywordTrendRepository.findFirstByOrderBySnapshotDateDescCreatedAtDesc()
            .map(GoogleKeywordTrend::getKeywords)
            .map(KeywordTrendResponse::from)
            .orElse(null)
    );
  }
}
