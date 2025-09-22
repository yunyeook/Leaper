package com.ssafy.spark.domain.business.platformAccount.service;

import com.ssafy.spark.domain.business.platformAccount.dto.response.PlatformAccountResponse;
import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.platformAccount.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;

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