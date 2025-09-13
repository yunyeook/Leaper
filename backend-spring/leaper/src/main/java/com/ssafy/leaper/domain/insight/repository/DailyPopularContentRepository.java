package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyPopularContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyPopularContentRepository extends JpaRepository<DailyPopularContent, Long> {

  List<DailyPopularContent> findByPlatformType_IdAndCategoryType_IdOrderBySnapshotDateDescContentRankAsc(
      String platformTypeId,
      Integer categoryTypeId
  );

  List<DailyPopularContent> findByPlatformType_IdOrderBySnapshotDateDescContentRankAsc(
      String platformTypeId
  );

  List<DailyPopularContent> findByCategoryType_IdOrderBySnapshotDateDescContentRankAsc(
      Integer categoryTypeId
  );

  List<DailyPopularContent> findAllByOrderBySnapshotDateDescContentRankAsc();
}
