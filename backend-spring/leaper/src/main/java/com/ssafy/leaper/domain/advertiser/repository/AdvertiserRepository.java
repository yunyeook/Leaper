package com.ssafy.leaper.domain.advertiser.repository;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdvertiserRepository extends JpaRepository<Advertiser, Integer> {

    boolean existsByLoginId(String loginId);

    boolean existsByBusinessRegNo(String businessRegNo);

    Optional<Advertiser> findByLoginId(String loginId);
}