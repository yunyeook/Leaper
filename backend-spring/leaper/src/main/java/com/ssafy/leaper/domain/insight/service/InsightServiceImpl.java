package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.AccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.DailyAccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.DailyAccountViewsResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.InfluencerViewsResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.MonthlyAccountInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.DailyAccountInsight.MonthlyAccountViewsResponse;
import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import com.ssafy.leaper.domain.insight.repository.DailyAccountInsightRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

  private final DailyAccountInsightRepository dailyAccountInsightRepository;

  // ✅ 인플루언서 단위 풀데이터
  @Override
  public ServiceResult<AccountInsightResponse> getAccountInsights(Long influencerId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByInfluencerId(influencerId);

    List<DailyAccountInsightResponse> dailyResponses = entities.stream()
        .map(DailyAccountInsightResponse::from)
        .toList();

    List<MonthlyAccountInsightResponse> monthlyResponses = aggregateToMonthly(entities);

    return ServiceResult.ok(AccountInsightResponse.of(dailyResponses, monthlyResponses));
  }

  // ✅ 플랫폼 계정 단위 풀데이터 (추가)
  @Override
  public ServiceResult<AccountInsightResponse> getPlatformAccountInsights(Long platformAccountId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByPlatformAccountId(platformAccountId);

    List<DailyAccountInsightResponse> dailyResponses = entities.stream()
        .map(DailyAccountInsightResponse::from)
        .toList();

    List<MonthlyAccountInsightResponse> monthlyResponses = aggregateToMonthly(entities);

    return ServiceResult.ok(AccountInsightResponse.of(dailyResponses, monthlyResponses));
  }

  // ✅ 인플루언서 단위 조회수 전용
  @Override
  public ServiceResult<InfluencerViewsResponse> getInfluencerViews(Long influencerId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByInfluencerId(influencerId);
    return ServiceResult.ok(aggregateDailyAndMonthlyViews(entities));
  }

  // ✅ 플랫폼 계정 단위 조회수 전용
  @Override
  public ServiceResult<InfluencerViewsResponse> getPlatformAccountViews(Long platformAccountId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByPlatformAccountId(platformAccountId);
    return ServiceResult.ok(aggregateDailyAndMonthlyViews(entities));
  }

  // 📌 공통: 풀데이터 → 월별 집계
  private List<MonthlyAccountInsightResponse> aggregateToMonthly(List<DailyAccountInsight> dailyInsights) {
    Map<String, DailyAccountInsight> latestByMonth = new HashMap<>();

    for (DailyAccountInsight dai : dailyInsights) {
      YearMonth ym = YearMonth.from(dai.getSnapshotDate());
      String key = dai.getPlatformAccount().getId() + "-" + ym;

      DailyAccountInsight existing = latestByMonth.get(key);
      if (existing == null || dai.getSnapshotDate().isAfter(existing.getSnapshotDate())) {
        latestByMonth.put(key, dai);
      }
    }

    return latestByMonth.values().stream()
        .map(dai -> MonthlyAccountInsightResponse.of(
            dai.getPlatformAccount().getId(),
            dai.getPlatformAccount().getPlatformType().getId(),
            dai.getTotalViews(),
            dai.getTotalFollowers(),
            dai.getTotalContents(),
            dai.getTotalLikes(),
            dai.getTotalComments(),
            YearMonth.from(dai.getSnapshotDate())
        ))
        .toList();
  }

  // 📌 공통: 조회수 전용 → Daily + Monthly
  private InfluencerViewsResponse aggregateDailyAndMonthlyViews(List<DailyAccountInsight> dailyInsights) {
    List<DailyAccountViewsResponse> dailyResponses = dailyInsights.stream()
        .map(DailyAccountViewsResponse::from)
        .toList();

    Map<String, DailyAccountInsight> latestByMonth = new HashMap<>();
    for (DailyAccountInsight dai : dailyInsights) {
      YearMonth ym = YearMonth.from(dai.getSnapshotDate());
      String key = dai.getPlatformAccount().getId() + "-" + ym;

      DailyAccountInsight existing = latestByMonth.get(key);
      if (existing == null || dai.getSnapshotDate().isAfter(existing.getSnapshotDate())) {
        latestByMonth.put(key, dai);
      }
    }

    List<MonthlyAccountViewsResponse> monthlyResponses = latestByMonth.values().stream()
        .map(dai -> MonthlyAccountViewsResponse.of(
            dai.getPlatformAccount().getId(),
            dai.getPlatformAccount().getPlatformType().getId(),
            dai.getTotalViews(),
            YearMonth.from(dai.getSnapshotDate())
        ))
        .toList();

    return InfluencerViewsResponse.of(dailyResponses, monthlyResponses);
  }
}
