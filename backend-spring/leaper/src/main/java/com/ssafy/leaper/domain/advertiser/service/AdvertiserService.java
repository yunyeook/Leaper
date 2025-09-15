package com.ssafy.leaper.domain.advertiser.service;

import com.ssafy.leaper.domain.advertiser.dto.request.AdvertiserSignupRequest;
import com.ssafy.leaper.domain.advertiser.dto.request.BusinessValidationApiRequest;
import com.ssafy.leaper.domain.advertiser.dto.response.AdvertiserSignupResponse;
import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.advertiser.repository.AdvertiserRepository;
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

    @Transactional
    public ServiceResult<AdvertiserSignupResponse> signup(AdvertiserSignupRequest request) {

        log.info("Starting advertiser signup process - loginId: {}, brandName: {}",
                request.getLoginId(), request.getBrandName());

        try {
            // 1. 중복 검증 - loginId
            if (advertiserRepository.existsByLoginId(request.getLoginId())) {
                log.warn("Advertiser signup failed - duplicate loginId: {}", request.getLoginId());
                return ServiceResult.fail(ErrorCode.DUPLICATE_LOGIN_ID);
            }

            // 2. 중복 검증 - 사업자등록번호
            if (advertiserRepository.existsByBusinessRegNo(request.getBusinessRegNo())) {
                log.warn("Advertiser signup failed - duplicate businessRegNo: {}", request.getBusinessRegNo());
                return ServiceResult.fail(ErrorCode.DUPLICATE_BUSINESS_REG_NO);
            }

            // 3. 실제 사업자등록번호 검증 (국세청 API)
            if (!businessValidationService.validateBusinessRegistration(
                    request.getBusinessRegNo(),
                    request.getRepresentativeName(),
                    request.getOpeningDate())) {
                log.warn("Advertiser signup failed - invalid business registration: businessRegNo: {}, representativeName: {}",
                        request.getBusinessRegNo(), request.getRepresentativeName());
                return ServiceResult.fail(ErrorCode.INVALID_BUSINESS_REG_NO);
            }

            // 3. 프로필 이미지 업로드 처리
            com.ssafy.leaper.domain.file.entity.File profileImage = handleProfileImageUpload(request.getCompanyProfileImage());

            // 4. 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // 5. 광고주 생성
            Advertiser advertiser = Advertiser.builder()
                    .loginId(request.getLoginId())
                    .password(encodedPassword)
                    .brandName(request.getBrandName())
                    .companyName(request.getBrandName()) // 회사명을 브랜드명으로 설정
                    .profileImage(profileImage)
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

            return ServiceResult.ok(AdvertiserSignupResponse.builder()
                    .advertiserId(savedAdvertiser.getId().toString())
                    .build());

        } catch (Exception e) {
            log.error("Failed to signup advertiser - loginId: {}, brandName: {}",
                    request.getLoginId(), request.getBrandName(), e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    private com.ssafy.leaper.domain.file.entity.File handleProfileImageUpload(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }

        // TODO: S3 업로드 로직 구현
        log.info("Profile image upload requested - filename: {}, size: {}",
                profileImage.getOriginalFilename(), profileImage.getSize());

        // 현재는 null 반환, 추후 File 엔티티와 S3 서비스 연동
        return null;
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

        try {
            boolean isValid = businessValidationService.validateBusinessRegistration(
                    request.getBusinessRegNo(),
                    request.getRepresentativeName(),
                    request.getOpeningDate()
            );

            if (isValid) {
                log.info("API business registration validation succeeded - businessRegNo: {}",
                        request.getBusinessRegNo());
                return ServiceResult.ok();
            } else {
                log.warn("API business registration validation failed - businessRegNo: {}",
                        request.getBusinessRegNo());
                return ServiceResult.fail(ErrorCode.INVALID_BUSINESS_REG_NO);
            }

        } catch (Exception e) {
            log.error("Failed to validate business registration via API - businessRegNo: {}, representativeName: {}",
                    request.getBusinessRegNo(), request.getRepresentativeName(), e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }
}