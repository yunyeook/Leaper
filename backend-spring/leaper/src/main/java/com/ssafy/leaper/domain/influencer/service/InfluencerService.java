package com.ssafy.leaper.domain.influencer.service;

import com.ssafy.leaper.domain.influencer.dto.request.InfluencerSignupRequest;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerSignupResponse;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.domain.type.entity.ProviderType;
import com.ssafy.leaper.domain.type.repository.ProviderTypeRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import com.ssafy.leaper.global.jwt.JwtValidationService;
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

            log.info("Influencer signup completed - influencerId: {}, nickname: {}",
                    savedInfluencer.getId(), savedInfluencer.getNickname());

            return ServiceResult.ok(InfluencerSignupResponse.from(savedInfluencer));

        } catch (Exception e) {
            log.error("Failed to signup influencer - nickname: {}, email: {}",
                    request.getNickname(), request.getEmail(), e);
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
}