package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.type.entity.CategoryType;
import com.ssafy.leaper.domain.type.entity.PlatformType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DailyTrendingInfluencer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_trending_influencer_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_type_id", nullable = false)
  private PlatformType platformType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "influencer_id", nullable = false)
  private Influencer influencer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_type_id", nullable = false)
  private CategoryType categoryType;

  @Column(nullable = false)
  private Integer influencerRank;

  @Column(nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}