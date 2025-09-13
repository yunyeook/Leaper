package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight.DailyTypeInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight.MonthlyTypeInsightResponse;
import com.ssafy.leaper.domain.insight.dto.response.dailyTypeInsight.TypeInsightResponse;
import com.ssafy.leaper.domain.insight.entity.DailyTypeInsight;
import com.ssafy.leaper.domain.insight.repository.DailyTypeInsightRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DailyTypeInsightService {

  private final DailyTypeInsightRepository repository;

  public ServiceResult<TypeInsightResponse> getTypeInsightsByInfluencer(Long influencerId, String contentTypeId) {
    List<DailyTypeInsight> entities = repository.findByInfluencerIdAndContentType(influencerId, contentTypeId);
    return ServiceResult.ok(aggregateDailyAndMonthly(entities));
  }

  public ServiceResult<TypeInsightResponse> getTypeInsightsByPlatformAccount(Long platformAccountId, String contentTypeId) {
    List<DailyTypeInsight> entities = repository.findByPlatformAccountIdAndContentType(platformAccountId, contentTypeId);
    return ServiceResult.ok(aggregateDailyAndMonthly(entities));
  }

  private TypeInsightResponse aggregateDailyAndMonthly(List<DailyTypeInsight> dailyInsights) {
    // Daily 변환
    List<DailyTypeInsightResponse> dailyResponses = dailyInsights.stream()
        .map(DailyTypeInsightResponse::from)
        .toList();

    // 월별 마지막 스냅샷만 선택
    Map<String, DailyTypeInsight> latestByMonth = new HashMap<>();
    for (DailyTypeInsight dti : dailyInsights) {
      YearMonth ym = YearMonth.from(dti.getSnapshotDate());
      String key = dti.getPlatformAccount().getId() + "-" + dti.getContentType().getId() + "-" + ym;

      DailyTypeInsight existing = latestByMonth.get(key);
      if (existing == null || dti.getSnapshotDate().isAfter(existing.getSnapshotDate())) {
        latestByMonth.put(key, dti);
      }
    }

    List<MonthlyTypeInsightResponse> monthlyResponses = latestByMonth.values().stream()
        .map(dti -> MonthlyTypeInsightResponse.of(
            dti.getPlatformAccount().getId(),
            dti.getPlatformAccount().getPlatformType().getId(),
            dti.getContentType().getId(),
            dti.getMonthViews(),
            dti.getMonthContents(),
            dti.getMonthLikes(),
            dti.getTotalViews(),
            dti.getTotalContents(),
            dti.getTotalLikes(),
            YearMonth.from(dti.getSnapshotDate())
        ))
        .toList();

    return TypeInsightResponse.of(dailyResponses, monthlyResponses);
  }
}
