package com.ssafy.spark.domain.crawling.instagram.repository;

import com.ssafy.spark.domain.crawling.instagram.entity.Influencer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfluencerRepository extends JpaRepository<Influencer, Integer> {
  Optional<Influencer> findByProviderMemberId(String providerMemberId);
}

