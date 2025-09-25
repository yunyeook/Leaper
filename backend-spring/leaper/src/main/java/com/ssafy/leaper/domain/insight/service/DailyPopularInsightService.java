package com.ssafy.leaper.domain.insight.service;

import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.insight.dto.response.dailyPopularInsight.*;
import com.ssafy.leaper.domain.insight.entity.DailyMyPopularContent;
import com.ssafy.leaper.domain.insight.entity.DailyPopularContent;
import com.ssafy.leaper.domain.insight.entity.DailyPopularInfluencer;
import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import com.ssafy.leaper.domain.insight.repository.DailyMyPopularContentRepository;
import com.ssafy.leaper.domain.insight.repository.DailyPopularContentRepository;
import com.ssafy.leaper.domain.insight.repository.DailyPopularInfluencerRepository;
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
public class DailyPopularInsightService {

  private final DailyPopularContentRepository dailyPopularContentRepository;
  private final DailyPopularInfluencerRepository dailyPopularInfluencerRepository;
  private final DailyAccountInsightRepository dailyAccountInsightRepository;
  private final DailyMyPopularContentRepository dailyMyPopularContentRepository;
  private final S3PresignedUrlService s3PresignedUrlService;


  /**
   * 플랫폼/카테고리별 인기 콘텐츠 조회 (최신 기준, TOP10)
   */
  public ServiceResult<DailyPopularContentResponse> getPopularContents(String platformTypeId, Long categoryTypeId) {


    List<DailyPopularContent> contents = dailyPopularContentRepository
        .findTop10ByPlatformAndCategoryAndDate(platformTypeId, categoryTypeId);

    List<PopularContentResponse> responses = contents.stream()
        .map(PopularContentResponse::from)
        .toList();

    return ServiceResult.ok(DailyPopularContentResponse.from(responses));
  }

  /**
   * 플랫폼/카테고리별 인기 인플루언서 조회 (오늘 기준, TOP10)
   */
  public ServiceResult<DailyPopularInfluencerResponse> getPopularInfluencers(String platformTypeId, Long categoryTypeId) {

    List<DailyPopularInfluencer> influencers = dailyPopularInfluencerRepository
        .findTop10ByPlatformAndCategoryAndDate(platformTypeId, categoryTypeId);

    List<PopularInfluencerResponse> responses = influencers.stream()
        .map(dpi -> {
          Integer totalFollowers = dailyAccountInsightRepository
              .findTopByPlatformAccountInfluencerIdOrderBySnapshotDateDesc(dpi.getInfluencer().getId())
              .map(DailyAccountInsight::getTotalFollowers)
              .orElse(0);
          return PopularInfluencerResponse.of(dpi, totalFollowers);
        })
        .toList();

    return ServiceResult.ok(DailyPopularInfluencerResponse.from(responses));
  }

  /**
   * 해당 계정의 인기 콘텐츠 조회
   */
  public ServiceResult<DailyMyPopularContentResponse> getMyPopularContents(Long platformAccountId) {

    List<DailyMyPopularContent> entities =
        dailyMyPopularContentRepository.findTop3ByLatestSnapshotDate(
            platformAccountId
        );
    List<MyPopularContentResponse> responses = entities.stream()
        .map(entity -> MyPopularContentResponse.of(entity, s3PresignedUrlService))
        .toList();

    return ServiceResult.ok(DailyMyPopularContentResponse.from(responses));
  }

}
