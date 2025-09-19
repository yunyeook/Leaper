package com.ssafy.leaper.domain.influencer.service;

import com.ssafy.leaper.domain.file.service.S3FileService;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.influencer.dto.request.InfluencerSignupRequest;
import com.ssafy.leaper.domain.influencer.dto.request.InfluencerUpdateRequest;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerProfileResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerPublicProfileResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerUpdateResponse;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerSignupResponse;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.domain.type.entity.ProviderType;
import com.ssafy.leaper.domain.type.repository.ProviderTypeRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.jwt.JwtValidationService;
import com.ssafy.leaper.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InfluencerService {

    private final InfluencerRepository influencerRepository;
    private final ProviderTypeRepository providerTypeRepository;
    private final JwtValidationService jwtValidationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final S3FileService s3FileService;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Transactional
    public ServiceResult<InfluencerSignupResponse> signup(
            InfluencerSignupRequest request,
            String authorizationHeader) {

        log.info("Starting influencer signup process - nickname: {}, email: {}",
                request.getNickname(), request.getEmail());

        try {
            // 1. JWT 토큰 검증 및 provider 정보 추출
            Jwt jwt;
            try {
                jwt = jwtValidationService.validateRegistrationToken(authorizationHeader);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid JWT token format: {}", e.getMessage());
                if (e.getMessage().contains("Authorization 헤더")) {
                    return ServiceResult.fail(ErrorCode.JWT_INVALID_HEADER);
                } else if (e.getMessage().contains("등록용 토큰")) {
                    return ServiceResult.fail(ErrorCode.JWT_INVALID_REGISTRATION_TOKEN);
                }
                return ServiceResult.fail(ErrorCode.JWT_INVALID_TOKEN);
            } catch (JwtException e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return ServiceResult.fail(ErrorCode.JWT_INVALID_TOKEN);
            }

            String providerMemberId = jwt.getClaimAsString("providerMemberId");
            String providerTypeId = jwt.getClaimAsString("providerTypeId");

            log.info("Registration token validated - providerTypeId: {}, providerMemberId: {}",
                    providerTypeId, providerMemberId);

            // 2. 중복 검증
            if (influencerRepository.existsByNickname(request.getNickname())) {
                return ServiceResult.fail(ErrorCode.DUPLICATE_NICKNAME);
            }

            // 같은 소셜 계정으로 이미 가입한 사용자가 있는지 확인
            if (influencerRepository.findByProviderTypeIdAndProviderMemberId(
                    providerTypeId, providerMemberId).isPresent()) {
                return ServiceResult.fail(ErrorCode.DUPLICATE_SOCIAL_ACCOUNT);
            }

            // 3. ProviderType 조회
            ProviderType providerType = providerTypeRepository.findById(providerTypeId)
                    .orElseThrow(() -> new RuntimeException("ProviderType not found: " + providerTypeId));

            // 4. 프로필 이미지 업로드 처리 (현재는 null로 처리, 추후 S3 연동)
            com.ssafy.leaper.domain.file.entity.File profileImage = handleProfileImageUpload(request.getProfileImage());

            // 5. 인플루언서 생성
            Influencer influencer = Influencer.of(
                    providerType,
                    providerMemberId,
                    request.getEmail(),
                    request.getNickname(),
                    request.getGenderAsBoolean(),
                    request.getBirthDate(),
                    request.getBio(),
                    profileImage
            );

            // 6. 저장
            Influencer savedInfluencer = influencerRepository.save(influencer);

            // 7. AccessToken 생성
            String accessToken = jwtTokenProvider.generateInfluencerToken(
                    savedInfluencer.getId().toString(),
                    savedInfluencer.getEmail());

            log.info("Influencer signup completed - influencerId: {}, nickname: {}",
                    savedInfluencer.getId(), savedInfluencer.getNickname());

            return ServiceResult.ok(InfluencerSignupResponse.from(savedInfluencer, accessToken));

        } catch (Exception e) {
            log.error("Failed to signup influencer - nickname: {}, email: {}",
                    request.getNickname(), request.getEmail(), e);
            return ServiceResult.fail(ErrorCode.COMMON_INTERNAL_ERROR);
        }
    }

    public ServiceResult<Void> checkNicknameDuplicate(String nickname) {
        log.info("Checking nickname duplicate - nickname: {}", nickname);

        if (influencerRepository.existsByNickname(nickname)) {
            log.warn("Nickname already exists - nickname: {}", nickname);
            return ServiceResult.fail(ErrorCode.DUPLICATE_NICKNAME);
        }

        log.info("Nickname is available - nickname: {}", nickname);
        return ServiceResult.ok();
    }

    public ServiceResult<InfluencerProfileResponse> getMyProfile(Integer influencerId) {
        log.info("Getting my influencer profile - influencerId: {}", influencerId);
        return getInfluencerProfile(influencerId);
    }

    public ServiceResult<InfluencerProfileResponse> getInfluencerProfile(Integer influencerId) {
        log.info("Getting influencer profile - influencerId: {}", influencerId);

        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
        if (influencer == null) {
            log.warn("Influencer not found - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        // S3 presigned URL 생성
        String profileImageUrl = null;
        if (influencer.getProfileImage() != null) {
            try {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(influencer.getProfileImage().getId());
                log.info("Generated presigned URL for profile image - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId());
            } catch (Exception e) {
                log.warn("Failed to generate presigned URL for profile image - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId(), e);
                profileImageUrl = null;
            }
        }

        log.info("Influencer profile retrieved successfully - influencerId: {}, nickname: {}",
                influencerId, influencer.getNickname());

        return ServiceResult.ok(InfluencerProfileResponse.from(influencer, profileImageUrl));
    }

    public ServiceResult<InfluencerPublicProfileResponse> getPublicProfile(Integer influencerId) {
        log.info("Getting public influencer profile - influencerId: {}", influencerId);

        Influencer influencer = influencerRepository.findByIdAndIsDeletedFalse(influencerId).orElse(null);
        if (influencer == null) {
            log.warn("Influencer not found or deleted - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        // S3 presigned URL 생성
        String profileImageUrl = null;
        if (influencer.getProfileImage() != null) {
            try {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(influencer.getProfileImage().getId());
                log.info("Generated presigned URL for public profile image - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId());
            } catch (Exception e) {
                log.warn("Failed to generate presigned URL for public profile image - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId(), e);
                profileImageUrl = null;
            }
        }

        log.info("Public influencer profile retrieved successfully - influencerId: {}, nickname: {}",
                influencerId, influencer.getNickname());

        return ServiceResult.ok(InfluencerPublicProfileResponse.from(influencer, profileImageUrl));
    }

    @Transactional
    public ServiceResult<Void> deleteAccount(Integer influencerId) {
        log.info("Starting influencer account deletion - influencerId: {}", influencerId);

        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
        if (influencer == null) {
            log.warn("Influencer not found for deletion - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        if (influencer.getIsDeleted()) {
            log.warn("Influencer already deleted - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_ALREADY_DELETED);
        }

        // Soft delete 수행
        influencer.delete();
        influencerRepository.save(influencer);

        log.info("Influencer account deleted successfully - influencerId: {}, nickname: {}",
                influencerId, influencer.getNickname());

        return ServiceResult.ok();
    }

    @Transactional
    public ServiceResult<InfluencerUpdateResponse> updateProfile(Integer influencerId, InfluencerUpdateRequest request) {
        log.info("Starting influencer profile update - influencerId: {}, nickname: {}",
                influencerId, request.getNickname());

        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
        if (influencer == null) {
            log.warn("Influencer not found for update - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        if (influencer.getIsDeleted()) {
            log.warn("Cannot update deleted influencer - influencerId: {}", influencerId);
            return ServiceResult.fail(ErrorCode.INFLUENCER_ALREADY_DELETED);
        }

        // 닉네임 중복 검사 (자신의 현재 닉네임이 아닌 경우)
        if (!influencer.getNickname().equals(request.getNickname()) &&
            influencerRepository.existsByNickname(request.getNickname())) {
            log.warn("Nickname already exists during update - nickname: {}", request.getNickname());
            return ServiceResult.fail(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 프로필 이미지 처리 (기존 이미지 교체)
        com.ssafy.leaper.domain.file.entity.File newProfileImage = influencer.getProfileImage();
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            // 새 이미지 업로드
            newProfileImage = handleProfileImageUpload(request.getProfileImage());
            log.info("New profile image uploaded for update - influencerId: {}, fileId: {}",
                    influencerId, newProfileImage != null ? newProfileImage.getId() : null);
        }

        // 프로필 정보 업데이트 (JPA 변경 감지)
        influencer.updateProfile(
            request.getNickname(),
            request.getEmail(),
            request.getBio(),
            request.getBirthDate(),
            request.getGenderAsBoolean(),
            newProfileImage
        );

        // S3 presigned URL 생성
        String profileImageUrl = null;
        if (influencer.getProfileImage() != null) {
            try {
                profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(influencer.getProfileImage().getId());
                log.info("Generated presigned URL for updated profile - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId());
            } catch (Exception e) {
                log.warn("Failed to generate presigned URL for updated profile - influencerId: {}, fileId: {}",
                        influencerId, influencer.getProfileImage().getId(), e);
                profileImageUrl = null;
            }
        }

        log.info("Influencer profile updated successfully - influencerId: {}, nickname: {}",
                influencerId, influencer.getNickname());

        return ServiceResult.ok(InfluencerUpdateResponse.from(influencer, profileImageUrl));
    }

    private com.ssafy.leaper.domain.file.entity.File handleProfileImageUpload(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }

        log.info("Profile image upload requested - filename: {}, size: {}",
                profileImage.getOriginalFilename(), profileImage.getSize());

        // S3에 파일 업로드 및 DB 저장
        return s3FileService.uploadFileToS3(profileImage, "business/profile");
    }

    public ServiceResult<Void> deleteAccountByEmail(String email) {
        influencerRepository.deleteByEmail(email);
        return ServiceResult.ok();
    }
}