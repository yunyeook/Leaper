package com.ssafy.leaper.domain.content.repository;

import com.ssafy.leaper.domain.content.entity.Content;
import java.util.Optional;
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
      "WHERE c.platformAccount.id = :platformAccountId " +
      "ORDER BY c.publishedAt DESC")
  List<Content> findByPlatformAccountIdWithContentType(@Param("platformAccountId") Long platformAccountId);

  @Query("SELECT c FROM Content c " +
      "JOIN FETCH c.contentType " +
      "LEFT JOIN FETCH c.thumbnail " +
      "WHERE c.id = :contentId")
  Optional<Content> findByIdWithDetails(@Param("contentId") Long contentId);

  @Query("SELECT dpc.contentRank FROM DailyPopularContent dpc " +
      "WHERE dpc.content.id = :contentId " +
      "AND dpc.snapshotDate = CURRENT_DATE")
  Optional<Integer> findContentRankByContentId(@Param("contentId") Long contentId);
}