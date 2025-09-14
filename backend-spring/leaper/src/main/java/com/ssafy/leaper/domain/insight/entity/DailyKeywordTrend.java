package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.type.entity.PlatformType;
import com.ssafy.leaper.global.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyKeywordTrend {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_keyword_trend_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_type_id", nullable = false)
  private PlatformType platformType;

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
