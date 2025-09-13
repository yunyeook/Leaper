package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DailyMyPopularContent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_my_popular_content_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_account_id", nullable = false)
  private PlatformAccount platformAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @Column(nullable = false)
  private Integer contentRank;

  @Column(nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}
