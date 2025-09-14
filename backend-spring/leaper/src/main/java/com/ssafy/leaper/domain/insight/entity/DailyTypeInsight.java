package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import com.ssafy.leaper.domain.type.entity.ContentType;
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
@Table(
    name = "daily_type_insight",
    indexes = {
        @Index(name = "idx_content_day", columnList = "platform_account_id, snapshot_date")
    }
)
public class DailyTypeInsight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_type_insight_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_type_id", nullable = false)
  private ContentType contentType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_account_id", nullable = false)
  private PlatformAccount platformAccount;

  @Column(nullable = false)
  private Integer todayViews;

  @Column(nullable = false)
  private Integer todayContents;

  @Column(nullable = false)
  private Integer todayLikes;

  @Column(nullable = false)
  private BigInteger monthViews;

  @Column(nullable = false)
  private Integer monthContents;

  @Column(nullable = false)
  private BigInteger monthLikes;

  @Column(nullable = false)
  private BigInteger totalViews;

  @Column(nullable = false)
  private Integer totalContents;

  @Column(nullable = false)
  private BigInteger totalLikes;

  @Column(nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}
