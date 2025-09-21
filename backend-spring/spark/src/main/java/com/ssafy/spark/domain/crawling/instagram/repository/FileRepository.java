package com.ssafy.spark.domain.crawling.instagram.repository;

import com.ssafy.spark.domain.crawling.instagram.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Integer> {
}