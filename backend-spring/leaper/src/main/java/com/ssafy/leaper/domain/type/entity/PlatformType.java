package com.ssafy.leaper.domain.type.entity;

import com.ssafy.leaper.domain.category.entity.CategoryType;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformType {

    @Id
    @Column(name = "platform_type_id", length = 31)
    private String platformTypeId;

    @Column(name = "type_name", nullable = false, length = 31)
    private String typeName;

    @Builder
    public PlatformType(String platformTypeId, String typeName) {
        this.platformTypeId = platformTypeId;
        this.typeName = typeName;
    }

  @Entity
  @Table(name = "platform_account")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class PlatformAccount {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "platform_account_id")
      private Long platformAccountId;

      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "influencer_id", nullable = false)
      private Influencer influencer;

      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "platform_type_id", nullable = false)
      private PlatformType platformType;

      @Column(name = "external_account_id", nullable = false, length = 320)
      private String externalAccountId;

      @Column(name = "account_nickname", nullable = false, length = 301)
      private String accountNickname;

      @Column(name = "account_url", nullable = false, length = 500)
      private String accountUrl;

      @Column(name = "account_profile_image_url", length = 500)
      private String accountProfileImageUrl;

      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "category_type_id")
      private CategoryType categoryType;

      @Column(name = "is_deleted", nullable = false)
      private Boolean isDeleted;

      @Column(name = "deleted_at")
      private LocalDateTime deletedAt;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @Column(name = "updated_at", nullable = false)
      private LocalDateTime updatedAt;

      @Builder
      public PlatformAccount(Influencer influencer, PlatformType platformType, String externalAccountId,
                            String accountNickname, String accountUrl, String accountProfileImageUrl,
                            CategoryType categoryType, Boolean isDeleted, LocalDateTime deletedAt) {
          this.influencer = influencer;
          this.platformType = platformType;
          this.externalAccountId = externalAccountId;
          this.accountNickname = accountNickname;
          this.accountUrl = accountUrl;
          this.accountProfileImageUrl = accountProfileImageUrl;
          this.categoryType = categoryType;
          this.isDeleted = isDeleted;
          this.deletedAt = deletedAt;
          this.createdAt = LocalDateTime.now();
          this.updatedAt = LocalDateTime.now();
      }

      @PrePersist
      protected void onCreate() {
          this.createdAt = LocalDateTime.now();
          this.updatedAt = LocalDateTime.now();
      }

      @PreUpdate
      protected void onUpdate() {
          this.updatedAt = LocalDateTime.now();
      }
  }
}
