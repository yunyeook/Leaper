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
}