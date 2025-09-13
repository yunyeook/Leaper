package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.*;
import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import com.ssafy.leaper.domain.insight.repository.DailyAccountInsightRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

  private final DailyAccountInsightRepository dailyAccountInsightRepository;

  @Override
  public ServiceResult<AccountInsightResponse> getAccountInsights(Long influencerId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByInfluencerId(influencerId);

//    if (entities.isEmpty()) {
//      return ServiceResult.fail(ErrorCode.ACCOUNT_INSIGHT_NOT_FOUND);
//    }

    // Daily 변환
    List<DailyAccountInsightResponse> dailyResponses = entities.stream()
        .map(DailyAccountInsightResponse::from)
        .toList();

    // Daily → Monthly 집계
    List<MonthlyAccountInsightResponse> monthlyResponses = aggregateToMonthly(entities);

    return ServiceResult.ok(AccountInsightResponse.of(dailyResponses, monthlyResponses));
  }

  @Override
  public ServiceResult<InfluencerViewsResponse> getInfluencerViews(Long influencerId) {
    List<DailyAccountInsight> entities = dailyAccountInsightRepository.findByInfluencerId(influencerId);

    // Daily 조회수만
    List<DailyAccountViewsResponse> dailyViews = entities.stream()
        .map(DailyAccountViewsResponse::from)
        .toList();

    // Monthly 조회수 집계
    List<MonthlyAccountViewsResponse> monthlyViews = aggregateToMonthlyViews(entities);

    return ServiceResult.ok(InfluencerViewsResponse.of(dailyViews, monthlyViews));
  }



  // 📌 공통: 풀데이터 월별 집계
  private List<MonthlyAccountInsightResponse> aggregateToMonthly(List<DailyAccountInsight> dailyInsights) {
    Map<String, Map<YearMonth, DailyAccountInsight>> map = new HashMap<>();

    for (DailyAccountInsight dai : dailyInsights) {
      YearMonth ym = YearMonth.from(dai.getSnapshotDate());
      String key = dai.getPlatformAccount().getId() + "-" + ym;

      map.computeIfAbsent(key, k -> new HashMap<>())
          .put(ym, dai);
    }

    return map.values().stream()
        .flatMap(m -> m.values().stream())
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

  // 📌 조회수 전용 월별 집계
  private List<MonthlyAccountViewsResponse> aggregateToMonthlyViews(List<DailyAccountInsight> dailyInsights) {
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
        .map(dai -> MonthlyAccountViewsResponse.of(
            dai.getPlatformAccount().getId(),
            dai.getPlatformAccount().getPlatformType().getId(),
            dai.getTotalViews(),
            YearMonth.from(dai.getSnapshotDate())
        ))
        .toList();
  }
}
