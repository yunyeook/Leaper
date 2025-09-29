package com.ssafy.spark.domain.business.file.repository;

import com.ssafy.spark.domain.business.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Integer> {

    /**
     * accessKey로 File 조회
     */
    Optional<File> findByAccessKey(String accessKey);

    /**
     * accessKey로 File 존재 여부 확인
     */
    boolean existsByAccessKey(String accessKey);
}