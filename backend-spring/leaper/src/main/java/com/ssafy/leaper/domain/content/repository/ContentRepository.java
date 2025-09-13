package com.ssafy.leaper.domain.content.repository;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.type.entity.CategoryType;
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

  @Query("SELECT pa.categoryType.id FROM Content c " +
      "JOIN c.platformAccount pa " +
      "WHERE c.id = :contentId")
  Optional<Long> findCategoryTypeById(@Param("contentId") Long contentId);

  @Query(value = """
SELECT c.* FROM content c
JOIN platform_account pa ON c.platform_account_id = pa.platform_account_id
JOIN daily_account_insight d ON d.platform_account_id = pa.platform_account_id
WHERE c.content_id != :contentId
  AND c.platform_type_id = :platformTypeId
  AND c.content_type_id = :contentTypeId
  AND pa.category_type_id = :categoryTypeId
 AND (c.duration_seconds IS NULL OR (c.duration_seconds BETWEEN :baseDuration - 300 AND :baseDuration + 300))ORDER BY d.total_followers DESC, c.published_at DESC
LIMIT 3
""", nativeQuery = true)
  List<Content> findSimilarByCategory(
      @Param("contentId") Long contentId,
      @Param("platformTypeId") String platformTypeId,
      @Param("contentTypeId") String contentTypeId,
      @Param("categoryTypeId") Long categoryTypeId,
      @Param("baseDuration") Integer baseDuration
  );
}