package com.ssafy.leaper.domain.platformAccount.service;

import com.ssafy.leaper.domain.crawling.dto.request.CrawlingRequest;
import com.ssafy.leaper.domain.file.entity.File;
import com.ssafy.leaper.domain.file.service.S3FileService;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.domain.platformAccount.dto.request.PlatformAccountCreateRequest;
import com.ssafy.leaper.domain.platformAccount.dto.response.PlatformAccountResponse;
import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import com.ssafy.leaper.domain.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.leaper.domain.type.entity.CategoryType;
import com.ssafy.leaper.domain.type.entity.PlatformType;
import com.ssafy.leaper.domain.type.repository.CategoryTypeRepository;
import com.ssafy.leaper.domain.type.repository.PlatformTypeRepository;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformAccountServiceImpl implements PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;
    private final InfluencerRepository influencerRepository;
    private final PlatformTypeRepository platformTypeRepository;
    private final CategoryTypeRepository categoryTypeRepository;
    private final S3FileService s3FileService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final RestTemplate restTemplate;

    @Value("${spark.server.host}")
    private String sparkServerHost;

    @Value("${spark.server.port}")
    private String sparkServerPort;

    @Value("${spark.api.key}")
    private String sparkApiKey;


    @Override
    public void createPlatformAccounts(Integer influencerId, List<PlatformAccountCreateRequest> requests) {
        log.info("플랫폼 계정 등록 시작 - 인플루언서 ID: {}, 계정 수: {}", influencerId, requests.size());

        // 인플루언서 조회
        Influencer influencer = influencerRepository.findByIdAndIsDeletedFalse(influencerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INFLUENCER_NOT_FOUND));

        for (PlatformAccountCreateRequest request : requests) {
            // 플랫폼 타입 조회
            PlatformType platformType = platformTypeRepository.findById(request.getPlatformTypeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_TYPE_NOT_FOUND));

            // 카테고리 타입 조회
            CategoryType categoryType = categoryTypeRepository.findById(request.getCategoryTypeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_TYPE_NOT_FOUND));

            // 프로필 이미지 파일 처리 (URL에서 다운로드)
            File profileImageFile = null;
            if (request.getAccountProfileImageUrl() != null && !request.getAccountProfileImageUrl().trim().isEmpty()) {
                try {
                    profileImageFile = s3FileService.uploadFileFromUrl(
                            request.getAccountProfileImageUrl(),
                            "platformAccount/profileImage"
                    );
                } catch (Exception e) {
                    log.warn("프로필 이미지 처리 실패: {}", request.getAccountProfileImageUrl(), e);
                }
            }

            // 플랫폼 계정 생성
            PlatformAccount platformAccount = PlatformAccount.builder()
                    .influencer(influencer)
                    .platformType(platformType)
                    .categoryType(categoryType)
                    .externalAccountId(request.getExternalAccountId())
                    .accountNickname(request.getAccountNickname())
                    .accountUrl(request.getAccountUrl() != null ? request.getAccountUrl() : "")
                    .accountProfileImage(profileImageFile)
                    .isDeleted(false)
                    .deletedAt(LocalDateTime.now()) // 기본값 설정
                    .build();

            platformAccountRepository.save(platformAccount);

            // 계정 연결시 Spark 서버에서 해당 계정 크롤링 및 S3와 DB 저장 로직 추가
            //TODO : 활성화 하기
//            triggerCrawlingAsync(platformAccount);

            log.info("플랫폼 계정 등록 완료 - 플랫폼: {}, 계정: {}",
                    request.getPlatformTypeId(), request.getAccountNickname());
        }

        log.info("모든 플랫폼 계정 등록 완료 - 인플루언서 ID: {}", influencerId);
    }

    /**
     * Spark 서버에 비동기로 크롤링 요청
     */
    @Async("taskExecutor")
    public void triggerCrawlingAsync(PlatformAccount platformAccount) {
        try {
            String sparkUrl = "http://" + sparkServerHost + ":" + sparkServerPort;

            // 크롤링 요청 DTO
            CrawlingRequest crawlingRequest = CrawlingRequest.from(platformAccount);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // 보내는 데이터는 JSON 형태
            headers.set("X-API-Key", sparkApiKey); // 아무나 Spark 서버에 크롤링 요청을 보낼 수 없게 막는 용도

            HttpEntity<CrawlingRequest> entity = new HttpEntity<>(crawlingRequest, headers);

            // Spark 서버에 POST 요청
            ResponseEntity<Boolean> response = restTemplate.postForEntity(
                sparkUrl + "/api/v1/crawling/start",
                entity,
                Boolean.class  // Boolean으로 변경
            );

            log.info("Spark 서버로 크롤링 요청 전송 완료 - 계정 ID: {}, 응답 상태: {}, 성공 여부: {}",
                platformAccount.getId(), response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            log.error("Spark 서버로 크롤링 요청 전송 실패 - 계정 ID: {}", platformAccount.getId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformAccountResponse> getPlatformAccountsByInfluencer(Integer influencerId) {
        log.info("플랫폼 계정 조회 시작 - 인플루언서 ID: {}", influencerId);

        // 인플루언서 조회
        Influencer influencer = influencerRepository.findByIdAndIsDeletedFalse(influencerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INFLUENCER_NOT_FOUND));

        // 해당 인플루언서의 삭제되지 않은 플랫폼 계정 조회
        List<PlatformAccount> platformAccounts = platformAccountRepository.findByInfluencerAndIsDeletedFalse(influencer);

        // DTO로 변환
        List<PlatformAccountResponse> responses = platformAccounts.stream()
                .map(account -> {
                    // 프로필 이미지 presigned URL 생성
                    String profileImageUrl = "";
                    if (account.getAccountProfileImage() != null) {
                        try {
                            profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                                    account.getAccountProfileImage().getId()
                            );
                        } catch (Exception e) {
                            log.warn("프로필 이미지 presigned URL 생성 실패 - 계정 ID: {}", account.getId(), e);
                        }
                    }

                    // 카테고리 타입 ID 추출
                    Short categoryTypeId = null;
                    if (account.getCategoryType() != null) {
                        categoryTypeId = account.getCategoryType().getId();
                    }

                    return PlatformAccountResponse.of(
                            account.getId(),
                            account.getPlatformType().getId(),
                            account.getExternalAccountId(),
                            account.getAccountNickname(),
                            account.getAccountUrl(),
                            profileImageUrl,
                            categoryTypeId
                    );
                })
                .collect(Collectors.toList());

        log.info("플랫폼 계정 조회 완료 - 인플루언서 ID: {}, 계정 수: {}", influencerId, responses.size());
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformAccountResponse getPlatformAccountById(Integer platformAccountId) {
        log.info("개별 플랫폼 계정 조회 시작 - 계정 ID: {}", platformAccountId);

        // 플랫폼 계정 조회
        PlatformAccount platformAccount = platformAccountRepository.findById(platformAccountId.longValue())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        // 삭제된 계정인지 확인
        if (platformAccount.getIsDeleted()) {
            throw new BusinessException(ErrorCode.PLATFORM_ACCOUNT_ALREADY_DELETED);
        }

        // 프로필 이미지 presigned URL 생성
        String profileImageUrl = "";
        if (platformAccount.getAccountProfileImage() != null) {
            try {
                String accessKey = platformAccount.getAccountProfileImage().getAccessKey();

                if (accessKey != null && accessKey.startsWith("raw_data")) {
                    // raw_data로 시작하면 presigned URL 생성
                    profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                        platformAccount.getAccountProfileImage().getId()
                    );
                } else {
                    // raw_data로 시작하지 않으면 accessKey 그대로 사용
                    profileImageUrl = accessKey;
                }
            } catch (Exception e) {
                log.warn("프로필 이미지 URL 처리 실패 - 계정 ID: {}", platformAccount.getId(), e);
            }
        }

        // 카테고리 타입 ID 추출
        Short categoryTypeId = null;
        if (platformAccount.getCategoryType() != null) {
            categoryTypeId = platformAccount.getCategoryType().getId();
        }

        PlatformAccountResponse response = PlatformAccountResponse.of(
                platformAccount.getId(),
                platformAccount.getPlatformType().getId(),
                platformAccount.getExternalAccountId(),
                platformAccount.getAccountNickname(),
                platformAccount.getAccountUrl(),
                profileImageUrl,
                categoryTypeId
        );

        log.info("개별 플랫폼 계정 조회 완료 - 계정 ID: {}", platformAccountId);
        return response;
    }

    @Override
    public void deletePlatformAccount(Integer influencerId, Integer platformAccountId) {
        log.info("플랫폼 계정 삭제 시작 - 인플루언서 ID: {}, 계정 ID: {}", influencerId, platformAccountId);

        // 플랫폼 계정 조회
        PlatformAccount platformAccount = platformAccountRepository.findById(platformAccountId.longValue())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_ACCOUNT_NOT_FOUND));

        // 이미 삭제된 계정인지 확인
        if (platformAccount.getIsDeleted()) {
            throw new BusinessException(ErrorCode.PLATFORM_ACCOUNT_ALREADY_DELETED);
        }

        // 소유권 검증: 해당 계정이 요청자의 소유인지 확인
        if (!platformAccount.getInfluencer().getId().equals(influencerId)) {
            throw new BusinessException(ErrorCode.PLATFORM_ACCOUNT_ACCESS_DENIED);
        }

        // Soft Delete 수행
        platformAccount = PlatformAccount.builder()
                .id(platformAccount.getId())
                .influencer(platformAccount.getInfluencer())
                .platformType(platformAccount.getPlatformType())
                .externalAccountId(platformAccount.getExternalAccountId())
                .accountNickname(platformAccount.getAccountNickname())
                .accountUrl(platformAccount.getAccountUrl())
                .accountProfileImage(platformAccount.getAccountProfileImage())
                .categoryType(platformAccount.getCategoryType())
                .createdAt(platformAccount.getCreatedAt())
                .updatedAt(platformAccount.getUpdatedAt())
                .isDeleted(true)
                .deletedAt(LocalDateTime.now())
                .build();

        platformAccountRepository.save(platformAccount);

        log.info("플랫폼 계정 삭제 완료 - 계정 ID: {}", platformAccountId);
    }
}