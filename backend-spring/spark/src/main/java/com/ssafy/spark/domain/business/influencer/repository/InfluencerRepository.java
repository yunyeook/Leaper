package com.ssafy.spark.domain.business.influencer.repository;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfluencerRepository extends JpaRepository<Influencer, Integer> {
}