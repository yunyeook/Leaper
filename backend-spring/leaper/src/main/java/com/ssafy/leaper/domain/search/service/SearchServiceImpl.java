package com.ssafy.leaper.domain.search.service;

import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final SearchRepository searchRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Override
    public ServiceResult<InfluencerSearchResponse> searchInfluencers(InfluencerSearchRequest request) {
        log.info("Searching influencers with criteria: keyword={}, platform={}, category={}, contentType={}, minFollowers={}, maxFollowers={}",
                request.getKeyword(), request.getPlatform(), request.getCategory(),
                request.getContentType(), request.getMinFollowers(), request.getMaxFollowers());

        try {
            List<InfluencerSearchResponse.InfluencerInfo> influencers = searchRepository.searchInfluencers(request);

            // S3 accessKey를 presigned URL로 변환
            List<InfluencerSearchResponse.InfluencerInfo> influencersWithPresignedUrls = influencers.stream()
                    .map(this::convertToPresignedUrls)
                    .collect(Collectors.toList());

            InfluencerSearchResponse response = InfluencerSearchResponse.from(influencersWithPresignedUrls);

            log.info("Found {} influencers matching search criteria", influencersWithPresignedUrls.size());
            return ServiceResult.ok(response);

        } catch (Exception e) {
            log.error("Failed to search influencers", e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    private InfluencerSearchResponse.InfluencerInfo convertToPresignedUrls(InfluencerSearchResponse.InfluencerInfo influencer) {
        try {
            // 인플루언서 프로필 이미지 File ID를 presigned URL로 변환
            String influencerPresignedUrl = null;
            if (influencer.influencerProfileImageUrl() != null) {
                try {
                    Integer fileId = Integer.parseInt(influencer.influencerProfileImageUrl());
                    influencerPresignedUrl = s3PresignedUrlService.generatePresignedDownloadUrl(fileId);
                } catch (Exception e) {
                    log.warn("Failed to generate presigned URL for influencer profile image: {}", influencer.influencerProfileImageUrl(), e);
                }
            }

            // 플랫폼 계정들의 프로필 이미지 File ID를 presigned URL로 변환
            List<InfluencerSearchResponse.PlatformAccountInfo> platformAccountsWithPresignedUrls = influencer.platformAccounts().stream()
                    .map(account -> {
                        String accountPresignedUrl = null;
                        if (account.accountProfileImageUrl() != null) {
                            try {
                                Integer fileId = Integer.parseInt(account.accountProfileImageUrl());
                                accountPresignedUrl = s3PresignedUrlService.generatePresignedDownloadUrl(fileId);
                            } catch (Exception e) {
                                log.warn("Failed to generate presigned URL for account profile image: {}", account.accountProfileImageUrl(), e);
                            }
                        }

                        return InfluencerSearchResponse.PlatformAccountInfo.of(
                                account.platformAccountId(),
                                account.platformTypeId(),
                                accountPresignedUrl, // presigned URL로 교체
                                account.accountNickname(),
                                account.accountURL(),
                                account.totalFollowers(),
                                account.categoryName()
                        );
                    })
                    .collect(Collectors.toList());

            return InfluencerSearchResponse.InfluencerInfo.of(
                    influencer.influencerId(),
                    influencerPresignedUrl, // presigned URL로 교체
                    influencer.nickname(),
                    platformAccountsWithPresignedUrls
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URLs for influencer: {}", influencer.influencerId(), e);
            // 에러 발생 시 원본 데이터 반환
            return influencer;
        }
    }
}