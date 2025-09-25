package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyPopularInfluencer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyPopularInfluencerRepository extends JpaRepository<DailyPopularInfluencer, Long> {

  @Query("""
        SELECT dpi FROM DailyPopularInfluencer dpi
        WHERE dpi.platformType.id = :platformTypeId
          AND dpi.categoryType.id = :categoryTypeId
          AND dpi.snapshotDate = :snapshotDate
        ORDER BY dpi.influencerRank ASC
        """)
  List<DailyPopularInfluencer> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId,
      @Param("snapshotDate") LocalDate snapshotDate
  );
  @Query("""
      SELECT dpi FROM DailyPopularInfluencer dpi
      WHERE dpi.platformType.id = :platformTypeId
        AND dpi.categoryType.id = :categoryTypeId
        AND dpi.snapshotDate = (
            SELECT MAX(dpi2.snapshotDate) 
            FROM DailyPopularInfluencer dpi2 
            WHERE dpi2.platformType.id = :platformTypeId 
              AND dpi2.categoryType.id = :categoryTypeId
        )
      ORDER BY dpi.influencerRank ASC
      """)
  List<DailyPopularInfluencer> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId
  );
}
