package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.dailyTrendingInsight.*;
import com.ssafy.leaper.domain.insight.entity.DailyTrendingContent;
import com.ssafy.leaper.domain.insight.entity.DailyTrendingInfluencer;
import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import com.ssafy.leaper.domain.insight.repository.DailyTrendingContentRepository;
import com.ssafy.leaper.domain.insight.repository.DailyTrendingInfluencerRepository;
import com.ssafy.leaper.domain.insight.repository.DailyAccountInsightRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DailyTrendingInsightService {

  private final DailyTrendingContentRepository dailyTrendingContentRepository;
  private final DailyTrendingInfluencerRepository dailyTrendingInfluencerRepository;
  private final DailyAccountInsightRepository dailyAccountInsightRepository;

  /**
   * 플랫폼/카테고리별 급상승 콘텐츠 조회 (오늘 기준, TOP10)
   */
  public ServiceResult<DailyTrendingContentResponse> getTrendingContents(String platformTypeId, Long categoryTypeId) {
    LocalDate today = LocalDate.now();

    List<DailyTrendingContent> contents = dailyTrendingContentRepository
        .findTop10ByPlatformAndCategoryAndDate(platformTypeId, categoryTypeId, today);

    List<TrendingContentResponse> responses = contents.stream()
        .map(TrendingContentResponse::from)
        .toList();

    return ServiceResult.ok(DailyTrendingContentResponse.from(responses));
  }

  /**
   * 플랫폼/카테고리별 급상승 인플루언서 조회 (오늘 기준, TOP10)
   */
  public ServiceResult<DailyTrendingInfluencerResponse> getTrendingInfluencers(String platformTypeId, Long categoryTypeId) {
    LocalDate today = LocalDate.now();

    List<DailyTrendingInfluencer> influencers = dailyTrendingInfluencerRepository
        .findTop10ByPlatformAndCategoryAndDate(platformTypeId, categoryTypeId, today);

    List<TrendingInfluencerResponse> responses = influencers.stream()
        .map(dpi -> {
          Integer totalFollowers = dailyAccountInsightRepository
              .findTopByPlatformAccountInfluencerIdOrderBySnapshotDateDesc(dpi.getInfluencer().getId())
              .map(DailyAccountInsight::getTotalFollowers)
              .orElse(0);
          return TrendingInfluencerResponse.of(dpi, totalFollowers);
        })
        .toList();

    return ServiceResult.ok(DailyTrendingInfluencerResponse.from(responses));
  }
  
}
