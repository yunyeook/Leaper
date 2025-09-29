package com.ssafy.spark.domain.business.influencer.service;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import com.ssafy.spark.domain.business.influencer.repository.InfluencerRepository;
import com.ssafy.spark.domain.business.type.entity.ProviderType;
import com.ssafy.spark.domain.business.type.repository.ProviderTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InfluencerService {

    private final InfluencerRepository influencerRepository;
    private final ProviderTypeRepository providerTypeRepository;
    private final Random random = new Random();

    /**
     * 인플루언서 생성
     */
    @Transactional
    public Influencer createInfluencer(String nickname, String bio, Integer profileImageId) {
        log.info("인플루언서 생성 시작 - nickname: {}", nickname);

        String providerMemberId = String.valueOf(System.currentTimeMillis());

        // ProviderType GOOGLE 조회
        ProviderType googleProviderType = providerTypeRepository.findById("GOOGLE")
                .orElseThrow(() -> new IllegalArgumentException("GOOGLE ProviderType을 찾을 수 없습니다"));

        // 랜덤 성별 생성 (1 또는 0)
        Boolean gender = random.nextBoolean();

        // 이메일 생성
        String email = nickname + "@gmail.com";

        // 기본 생일
        LocalDate birthday = LocalDate.of(2000, 1, 1);

        // 인플루언서 엔티티 생성
        Influencer influencer = Influencer.builder()
                .providerType(googleProviderType)
                .providerMemberId(providerMemberId)
                .nickname(nickname)
                .gender(gender)
                .birthday(birthday)
                .email(email)
                .bio(bio)
                .profileImageId(profileImageId)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Influencer savedInfluencer = influencerRepository.save(influencer);

        log.info("인플루언서 생성 완료 - id: {}, nickname: {}, gender: {}, email: {}",
                savedInfluencer.getId(), savedInfluencer.getNickname(), savedInfluencer.getGender(), savedInfluencer.getEmail());

        return savedInfluencer;
    }
}