package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

public interface DailyAccountInsightRepository extends JpaRepository<DailyAccountInsight, Long> {

  //인플루언서 id로 인사이트 조회
  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.influencer.id = :influencerId " +
      "ORDER BY dai.snapshotDate")
  List<DailyAccountInsight> findByInfluencerId(@Param("influencerId") Long influencerId);


    @Query("SELECT dai FROM DailyAccountInsight dai " +
        "JOIN FETCH dai.platformAccount pa " +
        "JOIN FETCH pa.platformType pt " +
        "WHERE pa.id = :platformAccountId " +
        "ORDER BY dai.snapshotDate ASC")
    List<DailyAccountInsight> findByPlatformAccountId(@Param("platformAccountId") Long platformAccountId);

}
