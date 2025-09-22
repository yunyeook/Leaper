package com.ssafy.spark.domain.business.platformAccount.entity;

import com.ssafy.spark.domain.business.influencer.entity.Influencer;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import lombok.*;
import com.ssafy.spark.domain.business.type.entity.CategoryType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PlatformAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "platform_account_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "influencer_id", nullable = false)
  private Influencer influencer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "platform_type_id", nullable = false)
  private PlatformType platformType;

  @Column( nullable = false, length = 320)
  private String externalAccountId;

  @Column( nullable = false, length = 301)
  private String accountNickname;

  @Column(nullable = false, length = 500)
  private String accountUrl;

  private Integer accountProfileImageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_type_id")
  private CategoryType categoryType;

  @Column(nullable = false)
  private Boolean isDeleted;

  @Column( nullable = false)
  private LocalDateTime deletedAt;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;
}