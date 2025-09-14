package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyKeywordTrend;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyKeywordTrendRepository extends JpaRepository<DailyKeywordTrend,Long> {
  Optional<DailyKeywordTrend> findFirstByPlatformTypeIdOrderBySnapshotDateDescCreatedAtDesc(String platformTypeId);

}
