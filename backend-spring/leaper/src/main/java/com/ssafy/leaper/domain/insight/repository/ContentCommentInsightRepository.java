package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.ContentCommentInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentCommentInsightRepository extends JpaRepository<ContentCommentInsight, Long> {

  @Query("""
        SELECT cci FROM ContentCommentInsight cci
        JOIN FETCH cci.content c
        WHERE cci.content.id = :contentId
        ORDER BY cci.snapshotDate DESC
        LIMIT 1
    """)
  Optional<ContentCommentInsight> findLatestByContentId(@Param("contentId") Long contentId);
}