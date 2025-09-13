package com.ssafy.leaper.domain.content.repository;

import com.ssafy.leaper.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

  @Query("SELECT c FROM Content c " +
      "JOIN FETCH c.contentType " +
      "LEFT JOIN FETCH c.thumbnail " +
      "WHERE c.platformAccount.platformAccountId = :platformAccountId " +
      "ORDER BY c.publishedAt DESC")
  List<Content> findByPlatformAccountIdWithContentType(@Param("platformAccountId") Long platformAccountId);
}