package com.ssafy.spark.domain.business.type.repository;

import com.ssafy.spark.domain.business.type.entity.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentTypeRepository extends JpaRepository<ContentType, String> {
}