package com.ssafy.leaper.domain.platformAccount.repository;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    boolean existsByIdAndIsDeletedFalse(Long id);

    List<PlatformAccount> findByInfluencerAndIsDeletedFalse(Influencer influencer);

    // 소프트삭제된 계정 조회 (복원용)
    Optional<PlatformAccount> findByInfluencerAndExternalAccountIdAndIsDeletedTrue(Influencer influencer, String externalAccountId);

    // 활성 계정 중복 체크
    boolean existsByInfluencerAndExternalAccountIdAndIsDeletedFalse(Influencer influencer, String externalAccountId);
}
