package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyTrendingInfluencer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTrendingInfluencerRepository extends JpaRepository<DailyTrendingInfluencer, Long> {

  @Query("""
        SELECT dti FROM DailyTrendingInfluencer dti
        WHERE dti.platformType.id = :platformTypeId
          AND dti.categoryType.id = :categoryTypeId
          AND dti.snapshotDate = :snapshotDate
        ORDER BY dti.influencerRank ASC
        """)
  List<DailyTrendingInfluencer> findTop10ByPlatformAndCategoryAndDate(
      @Param("platformTypeId") String platformTypeId,
      @Param("categoryTypeId") Long categoryTypeId,
      @Param("snapshotDate") LocalDate snapshotDate
  );
}