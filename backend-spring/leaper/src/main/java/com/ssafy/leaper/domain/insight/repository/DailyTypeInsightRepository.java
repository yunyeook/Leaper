package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyTypeInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailyTypeInsightRepository extends JpaRepository<DailyTypeInsight, Long> {

  @Query("""
      SELECT dti FROM DailyTypeInsight dti
      JOIN dti.platformAccount pa
      WHERE pa.influencer.id = :influencerId
      AND dti.contentType.id = :contentTypeId
      """)
  List<DailyTypeInsight> findByInfluencerIdAndContentType(
      @Param("influencerId") Long influencerId,
      @Param("contentTypeId") String contentTypeId
  );

  @Query("""
      SELECT dti FROM DailyTypeInsight dti
      WHERE dti.platformAccount.id = :platformAccountId
      AND dti.contentType.id = :contentTypeId
      """)
  List<DailyTypeInsight> findByPlatformAccountIdAndContentType(
      @Param("platformAccountId") Long platformAccountId,
      @Param("contentTypeId") String contentTypeId
  );
}
