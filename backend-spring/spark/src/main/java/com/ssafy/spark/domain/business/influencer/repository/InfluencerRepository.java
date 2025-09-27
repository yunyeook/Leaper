package com.ssafy.spark.domain.business.influencer.repository;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfluencerRepository extends JpaRepository<Influencer, Integer> {
    Optional<Influencer> findByProviderMemberId(String providerMemberId); // 여옥

}