//package com.ssafy.spark.domain.crawling.instagram.entity;
//
//import java.time.LocalDateTime;
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.EntityListeners;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//import lombok.AccessLevel;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//// PlatformAccount.java
//@Entity
//@Getter
//@Builder
//@EntityListeners(AuditingEntityListener.class)
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//public class PlatformAccount {
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  @Column(name = "platform_account_id")
//  private Integer id;
//
//  @Column(name = "influencer_id", nullable = false)
//  private Integer influencerId;
//
//  @Column(name = "platform_type_id", nullable = false, length = 31)
//  private String platformTypeId;
//
//  @Column(name = "external_account_id", nullable = false, length = 320)
//  private String externalAccountId;
//
//  @Column(name = "account_nickname", nullable = false, length = 301)
//  private String accountNickname;
//
//  @Column(name = "account_url", nullable = false, length = 500)
//  private String accountUrl;
//
//  @Column(name = "account_profile_image_id")
//  private Integer accountProfileImageId;
//
//  @Column(name = "category_type_id")
//  private Integer categoryTypeId;
//
//  @Column(nullable = false)
//  private Boolean isDeleted;
//
//  private LocalDateTime deletedAt;
//
//  @CreatedDate
//  private LocalDateTime createdAt;
//
//  @LastModifiedDate
//  private LocalDateTime updatedAt;
//}