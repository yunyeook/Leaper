package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import jakarta.persistence.*;
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
public class DailyAccountInsight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_account_insight_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_account_id", nullable = false)
  private PlatformAccount platformAccount;

  @Column(nullable = false)
  private BigInteger totalViews;

  @Column(nullable = false)
  private Integer totalFollowers;

  @Column(nullable = false)
  private Integer totalContents;

  @Column(nullable = false)
  private BigInteger totalLikes;

  @Column(nullable = false)
  private BigInteger totalComments;

  @Column(nullable = false)
  private BigInteger likeScore;

  @Column(nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}
