package com.ssafy.leaper.domain.insight.repository;

import com.ssafy.leaper.domain.insight.entity.DailyKeywordTrend;
import com.ssafy.leaper.domain.insight.entity.GoogleKeywordTrend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoogleKeywordTrendRepository extends JpaRepository<GoogleKeywordTrend, Long> {
  Optional<GoogleKeywordTrend> findFirstByOrderBySnapshotDateDescCreatedAtDesc();


}