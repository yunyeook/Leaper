package com.ssafy.spark.domain.business.type.repository;

import com.ssafy.spark.domain.business.type.entity.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformTypeRepository extends JpaRepository<PlatformType, String> {

    /**
     * platformTypeId로 PlatformType 조회
     */
    Optional<PlatformType> findById(String platformTypeId);

    /**
     * platformTypeId로 PlatformType 존재 여부 확인
     */
    boolean existsById(String platformTypeId);
}