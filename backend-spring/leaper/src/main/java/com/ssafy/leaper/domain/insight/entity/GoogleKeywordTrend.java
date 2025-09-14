package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.global.converter.StringListJsonConverter;
import jakarta.persistence.*;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class GoogleKeywordTrend {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "google_keyword_trend_id")
  private Integer id;

  @Convert(converter = StringListJsonConverter.class)
  @Column(name = "keywords_json", columnDefinition = "JSON")
  private List<String> keywords;


  @Column(name = "search_volume")
  private BigInteger searchVolume;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  @Column(name = "created_at")
  private LocalDateTime createdAt;
}