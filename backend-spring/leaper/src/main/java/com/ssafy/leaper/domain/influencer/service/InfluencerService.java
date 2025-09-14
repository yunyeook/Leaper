package com.ssafy.leaper.domain.influencer.service;

import com.ssafy.leaper.domain.influencer.dto.request.InfluencerSignupRequest;
import com.ssafy.leaper.domain.influencer.dto.response.InfluencerSignupResponse;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InfluencerService {

    private final InfluencerRepository influencerRepository;

    @Transactional
    public ServiceResult<InfluencerSignupResponse> signup(InfluencerSignupRequest request) {
        log.info("Starting influencer signup process - nickname: {}, provider: {}",
                request.getNickname(), request.getProviderTypeId());

        try {
            // 1. 중복 검증
            if (influencerRepository.existsByNickname(request.getNickname())) {
                return ServiceResult.fail(ErrorCode.DUPLICATE_NICKNAME);
            }

            // 같은 소셜 계정으로 이미 가입한 사용자가 있는지 확인
            if (influencerRepository.findByProviderTypeIdAndProviderMemberId(
                    request.getProviderTypeId(), request.getProviderMemberId()).isPresent()) {
                return ServiceResult.fail(ErrorCode.DUPLICATE_SOCIAL_ACCOUNT);
            }

            // 2. 프로필 이미지 업로드 처리 (현재는 null로 처리, 추후 S3 연동)
            com.ssafy.leaper.domain.file.entity.File profileImage = handleProfileImageUpload(request.getProfileImage());

            // 3. 인플루언서 생성
            Influencer influencer = Influencer.of(
                    request.getProviderTypeId(),
                    request.getProviderMemberId(),
                    request.getEmail(),
                    request.getNickname(),
                    request.getGenderAsInteger(),
                    request.getBirthDate(),
                    request.getBio(),
                    profileImage
            );

            // 4. 저장
            Influencer savedInfluencer = influencerRepository.save(influencer);

            log.info("Influencer signup completed - influencerId: {}, nickname: {}",
                    savedInfluencer.getInfluencerId(), savedInfluencer.getNickname());

            return ServiceResult.ok(InfluencerSignupResponse.from(savedInfluencer));

        } catch (Exception e) {
            log.error("Failed to signup influencer - nickname: {}, provider: {}",
                    request.getNickname(), request.getProviderTypeId(), e);
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