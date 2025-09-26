package com.ssafy.spark.domain.business.type.repository;

import com.ssafy.spark.domain.business.type.entity.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderTypeRepository extends JpaRepository<ProviderType, String> {

    Optional<ProviderType> findById(String id);
}