package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyPopularContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyPopularContentRepository extends JpaRepository<DailyPopularContent, Long> {

  @Query("""
        SELECT dpc FROM DailyPopularContent dpc
        WHERE dpc.platformType.id = :platformTypeId
          AND dpc.categoryType.id = :categoryTypeId
          AND dpc.snapshotDate = :snapshotDate
        ORDER BY dpc.contentRank ASC
        """)
  List<DailyPopularContent> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId,
      @Param("snapshotDate") LocalDate snapshotDate
  );
  @Query("""
      SELECT dpc FROM DailyPopularContent dpc
      WHERE dpc.platformType.id = :platformTypeId
        AND dpc.categoryType.id = :categoryTypeId
        AND dpc.snapshotDate = (
            SELECT MAX(dpc2.snapshotDate) 
            FROM DailyPopularContent dpc2 
            WHERE dpc2.platformType.id = :platformTypeId 
              AND dpc2.categoryType.id = :categoryTypeId
        )
      ORDER BY dpc.contentRank ASC
      """)
  List<DailyPopularContent> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId
  );
}
