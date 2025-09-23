package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyAccountInsight;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyAccountInsightRepository extends JpaRepository<DailyAccountInsight, Long> {

  //인플루언서 id로 인사이트 조회
  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.influencer.id = :influencerId " +
      "ORDER BY dai.snapshotDate")
  List<DailyAccountInsight> findByInfluencerId(@Param("influencerId") Integer influencerId);


    @Query("SELECT dai FROM DailyAccountInsight dai " +
        "JOIN FETCH dai.platformAccount pa " +
        "JOIN FETCH pa.platformType pt " +
        "WHERE pa.id = :platformAccountId " +
        "ORDER BY dai.snapshotDate ASC")
    List<DailyAccountInsight> findByPlatformAccountId(@Param("platformAccountId") Integer platformAccountId);


  @Query("""
        SELECT SUM(dai.totalFollowers)
        FROM DailyAccountInsight dai
        WHERE dai.platformAccount.influencer.id = :influencerId
          AND dai.snapshotDate = :snapshotDate
    """)
  Integer sumFollowersByInfluencerAndDate(Integer influencerId, LocalDate snapshotDate);


  Optional<DailyAccountInsight> findTopByPlatformAccountInfluencerIdOrderBySnapshotDateDesc(Integer influencerId);


  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.id = :platformAccountId " +
      "ORDER BY dai.snapshotDate DESC " +
      "LIMIT 1")
  Optional<DailyAccountInsight> findLatestByPlatformAccountId(@Param("platformAccountId") Integer platformAccountId);

  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.influencer.id = :influencerId " +
      "AND dai.snapshotDate = (" +
      "SELECT MAX(dai2.snapshotDate) " +
      "FROM DailyAccountInsight dai2 " +
      "WHERE dai2.platformAccount.id = dai.platformAccount.id" +
      ")" +
      "ORDER BY dai.snapshotDate DESC")
  List<DailyAccountInsight> findLatestByInfluencerId(@Param("influencerId") Integer influencerId);
  // 기존 메서드는 그대로 두고, 365일 제한 메서드 추가
  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.influencer.id = :influencerId " +
      "AND dai.snapshotDate >= :fromDate " +
      "ORDER BY dai.snapshotDate")
  List<DailyAccountInsight> findByInfluencerId(@Param("influencerId") Integer influencerId,
      @Param("fromDate") LocalDate fromDate);

  @Query("SELECT dai FROM DailyAccountInsight dai " +
      "JOIN FETCH dai.platformAccount pa " +
      "JOIN FETCH pa.platformType pt " +
      "WHERE pa.id = :platformAccountId " +
      "AND dai.snapshotDate >= :fromDate " +
      "ORDER BY dai.snapshotDate ASC")
  List<DailyAccountInsight> findByPlatformAccountId(@Param("platformAccountId") Integer platformAccountId,
      @Param("fromDate") LocalDate fromDate);
}
