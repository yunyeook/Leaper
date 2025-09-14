package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.content.entity.Content;
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
public class DailyTrendingContent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_trending_content_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_type_id", nullable = false)
  private PlatformType platformType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_type_id", nullable = false)
  private CategoryType categoryType;

  @Column(nullable = false)
  private Integer contentRank;

  @Column(nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}