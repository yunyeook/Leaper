package com.ssafy.spark.domain.crawling.instagram.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// Influencer.java
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Influencer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "influencer_id")
  private Integer id;

  @Column(name = "provider_type_id", nullable = false, length = 31)
  private String providerTypeId;

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

  @Column(name = "influencer_profile_image_id")
  private Integer profileImageId;

  @Column(length = 401)
  private String bio;

  @Column(nullable = false)
  private Boolean isDeleted;

  @Column
  private LocalDateTime deletedAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
