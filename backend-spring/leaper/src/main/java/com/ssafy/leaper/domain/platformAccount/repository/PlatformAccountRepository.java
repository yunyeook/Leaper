package com.ssafy.leaper.domain.platformAccount.repository;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    boolean existsByIdAndIsDeletedFalse(Long id);

    List<PlatformAccount> findByInfluencerAndIsDeletedFalse(Influencer influencer);
}
