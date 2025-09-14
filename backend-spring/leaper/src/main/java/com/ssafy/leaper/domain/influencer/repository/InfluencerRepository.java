package com.ssafy.leaper.domain.influencer.repository;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfluencerRepository extends JpaRepository<Influencer, Long> {

    Optional<Influencer> findByProviderTypeIdAndProviderMemberId(String providerTypeId, String providerMemberId);

    Optional<Influencer> findByProviderTypeIdAndProviderMemberIdAndIsDeletedFalse(String providerTypeId, String providerMemberId);

    boolean existsByNickname(String nickname);
}