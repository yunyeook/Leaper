package com.ssafy.spark.domain.business.platformAccount.service;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import com.ssafy.spark.domain.business.platformAccount.dto.response.PlatformAccountResponse;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import com.ssafy.spark.domain.business.type.entity.CategoryType;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.domain.business.type.repository.CategoryTypeRepository;
import com.ssafy.spark.domain.business.type.repository.PlatformTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformTypeRepository platformTypeRepository;
    private final CategoryTypeRepository categoryTypeRepository;

    /**
     * externalAccountId와 platformTypeId로 PlatformAccount 엔티티 조회 (내부 사용)
     */
    public Optional<PlatformAccount> findEntityByExternalAccountIdAndPlatformType(
            String externalAccountId, String platformTypeId) {

        return platformAccountRepository
                .findByExternalAccountIdAndPlatformTypeId(externalAccountId, platformTypeId);
    }

    /**
     * platformTypeId로 PlatformAccount 엔티티 조회 (내부 사용)
     */
    public Optional<PlatformAccount> findByPlatformTypeId(String platformTypeId) {
        return platformAccountRepository.findByPlatformTypeId(platformTypeId);
    }

    /**
     * YOUTUBE 플랫폼의 모든 PlatformAccount 조회
     */
    public List<PlatformAccount> getAllYoutubePlatformAccounts() {
        return platformAccountRepository.findAllByPlatformTypeId("YOUTUBE");
    }

    /**
     * PlatformAccount 생성
     */
    @Transactional
    public PlatformAccount createPlatformAccount(Influencer influencer, String externalAccountId,
                                                 String accountNickname, short categoryTypeId) {
        log.info("PlatformAccount 생성 시작 - externalAccountId: {}, accountNickname: {}",
                externalAccountId, accountNickname);

        // YOUTUBE PlatformType 조회
        PlatformType youtubePlatformType = platformTypeRepository.findById("YOUTUBE")
                .orElseThrow(() -> new IllegalArgumentException("YOUTUBE PlatformType을 찾을 수 없습니다"));

        CategoryType categoryType = categoryTypeRepository.findById(categoryTypeId)
                .orElseThrow(() -> new IllegalArgumentException("CategoryType을 찾을 수 없습니다: " + categoryTypeId));

        // PlatformAccount 엔티티 생성
        PlatformAccount platformAccount = PlatformAccount.builder()
                .influencer(influencer)
                .platformType(youtubePlatformType)
                .externalAccountId(externalAccountId)
                .accountNickname(accountNickname)
                .accountUrl("https://youtube.com/channel/" + externalAccountId)
                .categoryType(categoryType)
                .isDeleted(false)
                .deletedAt(LocalDateTime.now()) // 기본값 설정
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PlatformAccount savedPlatformAccount = platformAccountRepository.save(platformAccount);

        log.info("PlatformAccount 생성 완료 - id: {}, externalAccountId: {}",
                savedPlatformAccount.getId(), savedPlatformAccount.getExternalAccountId());

        return savedPlatformAccount;
    }

    /**
     * PlatformAccount의 프로필 이미지 ID 업데이트
     */
    @Transactional
    public void updateAccountProfileImageId(String externalAccountId, String platformTypeId, Integer fileId) {
        log.info("PlatformAccount 프로필 이미지 ID 업데이트 시작 - externalAccountId: {}, fileId: {}",
                externalAccountId, fileId);

        Optional<PlatformAccount> platformAccount = platformAccountRepository
                .findByExternalAccountIdAndPlatformTypeId(externalAccountId, platformTypeId);

        if (platformAccount.isPresent()) {
            platformAccount.get().setAccountProfileImageId(fileId);
            platformAccountRepository.save(platformAccount.get()); // 명시적으로 save 호출
            log.info("PlatformAccount 프로필 이미지 ID 업데이트 완료 - externalAccountId: {}, fileId: {}",
                    externalAccountId, fileId);
        } else {
            log.warn("PlatformAccount를 찾을 수 없습니다 - externalAccountId: {}, platformTypeId: {}",
                    externalAccountId, platformTypeId);
            throw new IllegalArgumentException("PlatformAccount를 찾을 수 없습니다: " + externalAccountId);
        }
    }
}