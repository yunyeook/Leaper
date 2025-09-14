package com.ssafy.leaper.domain.influencer.entity;

import com.ssafy.leaper.domain.file.entity.File;
import com.ssafy.leaper.domain.type.entity.ProviderType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Influencer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "influencer_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_type_id", nullable = false)
  private ProviderType providerType;

  @Column(name = "provider_member_id", length = 31, nullable = false)
  private String providerMemberId;

  @Column(nullable = false, length = 61, unique = true)
  private String nickname;

  @Column(nullable = false)
  private Boolean gender;

  @Column(nullable = false)
  private LocalDate birthday;

  @Column(nullable = false, length = 320)
  private String email;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "influencer_profile_image_id")
  private File profileImage;

  @Column(length = 401)
  private String bio;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private Boolean isDeleted;

  private LocalDateTime deletedAt;

  public static Influencer of(ProviderType providerType, String providerMemberId, String email,
                              String nickname, Boolean gender, LocalDate birthday, String bio,
                              File profileImage) {
    return Influencer.builder()
            .providerType(providerType)
            .providerMemberId(providerMemberId)
            .email(email)
            .nickname(nickname)
            .gender(gender)
            .birthday(birthday)
            .bio(bio)
            .profileImage(profileImage)
            .isDeleted(false)
            .build();
  }
}

