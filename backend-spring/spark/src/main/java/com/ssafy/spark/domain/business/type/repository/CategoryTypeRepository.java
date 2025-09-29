package com.ssafy.spark.domain.business.type.repository;

import com.ssafy.spark.domain.business.type.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryTypeRepository extends JpaRepository<CategoryType, Short> {

    /**
     * categoryTypeId로 CategoryType 조회
     */
    Optional<CategoryType> findById(Short categoryTypeId);

    Optional<CategoryType> findById(Integer categoryTypeId);

    /**
     * categoryTypeId로 CategoryType 존재 여부 확인
     */
    boolean existsById(Short categoryTypeId);
}