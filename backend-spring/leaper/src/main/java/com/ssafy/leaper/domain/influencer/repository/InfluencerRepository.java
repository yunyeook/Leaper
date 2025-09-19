package com.ssafy.leaper.domain.influencer.repository;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface InfluencerRepository extends JpaRepository<Influencer, Integer> {

    Optional<Influencer> findByProviderTypeIdAndProviderMemberId(String providerTypeId, String providerMemberId);

    Optional<Influencer> findByProviderTypeIdAndProviderMemberIdAndIsDeletedFalse(String providerTypeId, String providerMemberId);

    boolean existsByNickname(String nickname);

    Optional<Influencer> findByIdAndIsDeletedFalse(Integer id);

    @Modifying
    @Transactional
    void deleteByEmail(String email);
}