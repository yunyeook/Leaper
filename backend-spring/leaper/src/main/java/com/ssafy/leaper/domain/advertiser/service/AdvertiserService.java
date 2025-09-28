package com.ssafy.leaper.domain.advertiser.service;

import com.ssafy.leaper.domain.advertiser.dto.request.AdvertiserSignupRequest;
import com.ssafy.leaper.domain.advertiser.dto.request.AdvertiserUpdateRequest;
import com.ssafy.leaper.domain.advertiser.dto.request.BusinessValidationApiRequest;
import com.ssafy.leaper.domain.advertiser.dto.response.AdvertiserMyProfileResponse;
import com.ssafy.leaper.domain.advertiser.dto.response.AdvertiserPublicProfileResponse;
import com.ssafy.leaper.domain.advertiser.dto.response.AdvertiserUpdateResponse;
import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.advertiser.repository.AdvertiserRepository;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.file.service.S3FileService;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdvertiserService {

    private final AdvertiserRepository advertiserRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessValidationService businessValidationService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final S3FileService s3FileService;

    @Transactional
    public ServiceResult<Void> signup(AdvertiserSignupRequest request) {

        log.info("Starting advertiser signup process - loginId: {}, brandName: {}",
                request.getLoginId(), request.getBrandName());

        try {
            // 1. 중복 검증 - loginId
            if (advertiserRepository.existsByLoginId(request.getLoginId())) {
                log.warn("Advertiser signup failed - duplicate loginId: {}", request.getLoginId());
                return ServiceResult.fail(ErrorCode.DUPLICATE_LOGIN_ID);
            }

            // 2. 중복 검증 - 사업자등록번호
//            if (advertiserRepository.existsByBusinessRegNo(request.getBusinessRegNo())) {
//                log.warn("Advertiser signup failed - duplicate businessRegNo: {}", request.getBusinessRegNo());
//                return ServiceResult.fail(ErrorCode.DUPLICATE_BUSINESS_REG_NO);
//            }

            // 3. 실제 사업자등록번호 검증 (국세청 API)
//            if (!businessValidationService.validateBusinessRegistration(
//                    request.getBusinessRegNo(),
//                    request.getRepresentativeName(),
//                    request.getOpeningDate())) {
//                log.warn("Advertiser signup failed - invalid business registration: businessRegNo: {}, representativeName: {}",
//                        request.getBusinessRegNo(), request.getRepresentativeName());
//                return ServiceResult.fail(ErrorCode.INVALID_BUSINESS_REG_NO);
//            }

            // 3. 프로필 이미지 업로드 처리
            com.ssafy.leaper.domain.file.entity.File companyProfileImage = handleProfileImageUpload(request.getCompanyProfileImage());

            // 4. 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // 5. 광고주 생성
            Advertiser advertiser = Advertiser.builder()
                    .loginId(request.getLoginId())
                    .password(encodedPassword)
                    .brandName(request.getBrandName())
                    .companyName(request.getBrandName()) // 회사명을 브랜드명으로 설정
                    .companyProfileImage(companyProfileImage)
                    .representativeName(request.getRepresentativeName())
                    .businessRegNo(request.getBusinessRegNo())
                    .bio(request.getBio())
                    .openingDate(request.getOpeningDate())
                    .isDeleted(false)
                    .deletedAt(null)
                    .build();

            // 6. 저장
            Advertiser savedAdvertiser = advertiserRepository.save(advertiser);

            log.info("Advertiser signup completed - advertiserId: {}, loginId: {}",
                    savedAdvertiser.getId(), savedAdvertiser.getLoginId());

            return ServiceResult.ok();

        } catch (Exception e) {
            log.error("Failed to signup advertiser - loginId: {}, brandName: {}",
                    request.getLoginId(), request.getBrandName(), e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    private com.ssafy.leaper.domain.file.entity.File handleProfileImageUpload(MultipartFile companyProfileImage) {
        if (companyProfileImage == null || companyProfileImage.isEmpty()) {
            return null;
        }

        log.info("Profile image upload requested - filename: {}, size: {}",
                companyProfileImage.getOriginalFilename(), companyProfileImage.getSize());

        // S3에 파일 업로드 및 DB 저장
        return s3FileService.uploadFileToS3(companyProfileImage, "business/profile");
    }

    public ServiceResult<Void> checkLoginIdDuplicate(String loginId) {
        log.info("Checking loginId duplicate - loginId: {}", loginId);

        try {
            if (advertiserRepository.existsByLoginId(loginId)) {
                log.warn("LoginId duplicate check failed - duplicate loginId: {}", loginId);
                return ServiceResult.fail(ErrorCode.DUPLICATE_LOGIN_ID);
            }

            log.info("LoginId duplicate check passed - loginId: {}", loginId);
            return ServiceResult.ok();

        } catch (Exception e) {
            log.error("Failed to check loginId duplicate - loginId: {}", loginId, e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    public ServiceResult<Void> validateBusinessRegistrationApi(BusinessValidationApiRequest request) {
        log.info("API business registration validation - businessRegNo: {}, representativeName: {}",
                request.getBusinessRegNo(), request.getRepresentativeName());

        return ServiceResult.ok();
//
//        try {
//            boolean isValid = businessValidationService.validateBusinessRegistration(
//                    request.getBusinessRegNo(),
//                    request.getRepresentativeName(),
//                    request.getOpeningDate()
//            );
//
//            if (isValid) {
//                log.info("API business registration validation succeeded - businessRegNo: {}",
//                        request.getBusinessRegNo());
//                return ServiceResult.ok();
//            } else {
//                log.warn("API business registration validation failed - businessRegNo: {}",
//                        request.getBusinessRegNo());
//                return ServiceResult.fail(ErrorCode.INVALID_BUSINESS_REG_NO);
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to validate business registration via API - businessRegNo: {}, representativeName: {}",
//                    request.getBusinessRegNo(), request.getRepresentativeName(), e);
//            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
//        }
    }

    public ServiceResult<AdvertiserMyProfileResponse> getMyProfile(Integer advertiserId) {
        log.info("Getting advertiser profile - advertiserId: {}", advertiserId);

        try {
            Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
            if (advertiser == null) {
                log.warn("Advertiser not found - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
            }

            String profileImageUrl = null;
            if (advertiser.getCompanyProfileImage() != null) {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                    advertiser.getCompanyProfileImage().getId()
                );
            }

            AdvertiserMyProfileResponse response = AdvertiserMyProfileResponse.from(advertiser, profileImageUrl);

            log.info("Successfully retrieved advertiser profile - advertiserId: {}", advertiserId);
            return ServiceResult.ok(response);

        } catch (Exception e) {
            log.error("Failed to get advertiser profile - advertiserId: {}", advertiserId, e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    public ServiceResult<AdvertiserPublicProfileResponse> getAdvertiserPublicProfile(Integer advertiserId) {
        log.info("Getting advertiser public profile - advertiserId: {}", advertiserId);

        try {
            Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
            if (advertiser == null) {
                log.warn("Advertiser not found - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
            }

            String profileImageUrl = null;
            if (advertiser.getCompanyProfileImage() != null) {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                    advertiser.getCompanyProfileImage().getId()
                );
            }

            AdvertiserPublicProfileResponse response = AdvertiserPublicProfileResponse.from(advertiser, profileImageUrl);

            log.info("Successfully retrieved advertiser public profile - advertiserId: {}", advertiserId);
            return ServiceResult.ok(response);

        } catch (Exception e) {
            log.error("Failed to get advertiser public profile - advertiserId: {}", advertiserId, e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    @Transactional
    public ServiceResult<Void> deleteAdvertiser(Integer advertiserId) {
        log.info("Starting advertiser deletion - advertiserId: {}", advertiserId);

        try {
            Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
            if (advertiser == null) {
                log.warn("Advertiser not found for deletion - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
            }

            if (advertiser.getIsDeleted()) {
                log.warn("Advertiser already deleted - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_ALREADY_DELETED);
            }

            advertiser = Advertiser.builder()
                    .id(advertiser.getId())
                    .loginId(advertiser.getLoginId())
                    .password(advertiser.getPassword())
                    .brandName(advertiser.getBrandName())
                    .companyName(advertiser.getCompanyName())
                    .companyProfileImage(advertiser.getCompanyProfileImage())
                    .representativeName(advertiser.getRepresentativeName())
                    .businessRegNo(advertiser.getBusinessRegNo())
                    .bio(advertiser.getBio())
                    .openingDate(advertiser.getOpeningDate())
                    .createdAt(advertiser.getCreatedAt())
                    .updatedAt(advertiser.getUpdatedAt())
                    .isDeleted(true)
                    .deletedAt(java.time.LocalDateTime.now())
                    .build();

            advertiserRepository.save(advertiser);

            log.info("Advertiser deletion completed - advertiserId: {}", advertiserId);
            return ServiceResult.ok();

        } catch (Exception e) {
            log.error("Failed to delete advertiser - advertiserId: {}", advertiserId, e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    @Transactional
    public ServiceResult<AdvertiserUpdateResponse> updateAdvertiser(Integer advertiserId, AdvertiserUpdateRequest request) {
        log.info("Starting advertiser update process - advertiserId: {}", advertiserId);

        try {
            Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
            if (advertiser == null) {
                log.warn("Advertiser not found for update - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
            }

            if (advertiser.getIsDeleted()) {
                log.warn("Cannot update deleted advertiser - advertiserId: {}", advertiserId);
                return ServiceResult.fail(ErrorCode.ADVERTISER_ALREADY_DELETED);
            }

            // 사업자등록번호가 변경된 경우 중복 체크 및 검증
            if (request.getBusinessRegNo() != null && !request.getBusinessRegNo().equals(advertiser.getBusinessRegNo())) {
                if (advertiserRepository.existsByBusinessRegNo(request.getBusinessRegNo())) {
                    log.warn("Business registration number already exists - businessRegNo: {}", request.getBusinessRegNo());
                    return ServiceResult.fail(ErrorCode.DUPLICATE_BUSINESS_REG_NO);
                }

                // 사업자등록번호 검증 (국세청 API)
                if (!businessValidationService.validateBusinessRegistration(
                        request.getBusinessRegNo(),
                        request.getRepresentativeName() != null ? request.getRepresentativeName() : advertiser.getRepresentativeName(),
                        request.getOpeningDate() != null ? request.getOpeningDate() : advertiser.getOpeningDate())) {
                    log.warn("Invalid business registration - businessRegNo: {}", request.getBusinessRegNo());
                    return ServiceResult.fail(ErrorCode.INVALID_BUSINESS_REG_NO);
                }
            }

            // 프로필 이미지 업로드 처리
            com.ssafy.leaper.domain.file.entity.File updatedProfileImage = advertiser.getCompanyProfileImage();
            if (request.getCompanyProfileImage() != null && !request.getCompanyProfileImage().isEmpty()) {
                updatedProfileImage = handleProfileImageUpload(request.getCompanyProfileImage());
            }

            // 광고주 정보 업데이트
            advertiser = Advertiser.builder()
                    .id(advertiser.getId())
                    .loginId(advertiser.getLoginId())
                    .password(advertiser.getPassword())
                    .brandName(request.getBrandName() != null ? request.getBrandName() : advertiser.getBrandName())
                    .companyName(request.getBrandName() != null ? request.getBrandName() : advertiser.getCompanyName())
                    .companyProfileImage(updatedProfileImage)
                    .representativeName(request.getRepresentativeName() != null ? request.getRepresentativeName() : advertiser.getRepresentativeName())
                    .businessRegNo(request.getBusinessRegNo() != null ? request.getBusinessRegNo() : advertiser.getBusinessRegNo())
                    .bio(request.getBio() != null ? request.getBio() : advertiser.getBio())
                    .openingDate(request.getOpeningDate() != null ? request.getOpeningDate() : advertiser.getOpeningDate())
                    .createdAt(advertiser.getCreatedAt())
                    .updatedAt(advertiser.getUpdatedAt())
                    .isDeleted(advertiser.getIsDeleted())
                    .deletedAt(advertiser.getDeletedAt())
                    .build();

            Advertiser updatedAdvertiser = advertiserRepository.save(advertiser);

            // 응답 생성
            String profileImageUrl = null;
            if (updatedAdvertiser.getCompanyProfileImage() != null) {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(
                    updatedAdvertiser.getCompanyProfileImage().getId()
                );
            }

            AdvertiserUpdateResponse response = AdvertiserUpdateResponse.from(updatedAdvertiser, profileImageUrl);

            log.info("Advertiser update completed - advertiserId: {}", advertiserId);
            return ServiceResult.ok(response);

        } catch (Exception e) {
            log.error("Failed to update advertiser - advertiserId: {}", advertiserId, e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }
}