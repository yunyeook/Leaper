package com.ssafy.leaper.domain.platformAccount.entity;

import com.ssafy.leaper.domain.file.entity.File;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
  private Long id;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_profile_image_id")
  private File accountProfileImage;

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