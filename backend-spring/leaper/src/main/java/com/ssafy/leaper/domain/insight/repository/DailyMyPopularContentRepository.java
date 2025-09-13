package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyMyPopularContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyMyPopularContentRepository extends JpaRepository<DailyMyPopularContent, Long> {

  List<DailyMyPopularContent> findByPlatformAccountIdAndSnapshotDateOrderByContentRankAsc(
      Long platformAccountId,
      LocalDate snapshotDate
  );
}
