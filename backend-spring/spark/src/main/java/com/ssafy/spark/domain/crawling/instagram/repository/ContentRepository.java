package com.ssafy.spark.domain.crawling.instagram.repository;

import com.ssafy.spark.domain.crawling.instagram.entity.Content;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Integer> {
  List<Content> findByPlatformAccountId(Integer platformAccountId);
  Optional<Content> findByExternalContentId(String externalContentId);
}