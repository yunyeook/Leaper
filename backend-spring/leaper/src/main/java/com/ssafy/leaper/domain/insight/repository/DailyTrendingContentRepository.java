package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyTrendingContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTrendingContentRepository extends JpaRepository<DailyTrendingContent, Long> {

  @Query("""
        SELECT dtc FROM DailyTrendingContent dtc
        WHERE dtc.platformType.id = :platformTypeId
          AND dtc.categoryType.id = :categoryTypeId
          AND dtc.snapshotDate = :snapshotDate
        ORDER BY dtc.contentRank ASC
        """)
  List<DailyTrendingContent> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId,
      @Param("snapshotDate") LocalDate snapshotDate
  );
  @Query("""
      SELECT dtc FROM DailyTrendingContent dtc
      WHERE dtc.platformType.id = :platformTypeId
        AND dtc.categoryType.id = :categoryTypeId
        AND dtc.snapshotDate = (
            SELECT MAX(dtc2.snapshotDate) 
            FROM DailyTrendingContent dtc2 
            WHERE dtc2.platformType.id = :platformTypeId 
              AND dtc2.categoryType.id = :categoryTypeId
        )
      ORDER BY dtc.contentRank ASC
      """)
  List<DailyTrendingContent> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId
  );
}