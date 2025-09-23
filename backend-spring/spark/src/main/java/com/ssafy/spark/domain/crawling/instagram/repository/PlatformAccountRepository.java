//package com.ssafy.spark.domain.crawling.instagram.repository;
//
//import com.ssafy.spark.domain.crawling.instagram.entity.PlatformAccount;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Integer> {
//  List<PlatformAccount> findByInfluencerId(Integer influencerId);
//  Optional<PlatformAccount> findByExternalAccountId(String externalAccountId);
//  List<PlatformAccount> findByPlatformTypeId(String platformTypeId);
//  Optional<PlatformAccount> findByAccountNickname(String username);
//}