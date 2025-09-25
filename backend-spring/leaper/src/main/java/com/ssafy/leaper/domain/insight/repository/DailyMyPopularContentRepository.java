package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyMyPopularContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyMyPopularContentRepository extends JpaRepository<DailyMyPopularContent, Long> {

  List<DailyMyPopularContent> findByPlatformAccountIdAndSnapshotDateOrderByContentRankAsc(
      Long platformAccountId,
      LocalDate snapshotDate
  );
  @Query("SELECT d FROM DailyMyPopularContent d " +
      "JOIN FETCH d.platformAccount " +
      "WHERE d.platformAccount.id = :platformAccountId " +
      "AND d.snapshotDate = (SELECT MAX(d2.snapshotDate) FROM DailyMyPopularContent d2 " +
      "WHERE d2.platformAccount.id = :platformAccountId) " +
      "ORDER BY d.contentRank ASC")
  List<DailyMyPopularContent> findTop3ByLatestSnapshotDate(@Param("platformAccountId") Long platformAccountId);

}
