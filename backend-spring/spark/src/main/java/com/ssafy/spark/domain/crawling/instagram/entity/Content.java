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

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Content {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "content_id")
  private Integer id;

  @Column(name = "platform_account_id", nullable = false)
  private Integer platformAccountId;

  @Column(name = "platform_type_id", nullable = false, length = 31)
  private String platformTypeId;

  @Column(name = "content_type_id", nullable = false, length = 31)
  private String contentTypeId;

  @Column(name = "external_content_id", nullable = false, length = 320)
  private String externalContentId;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "thumbnail_id")
  private Integer thumbnailId;

  @Column(name = "content_url", nullable = false, length = 500)
  private String contentUrl;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "tags_json", columnDefinition = "JSON")
  private String tagsJson;

  @Column(name = "total_views", nullable = false)
  private Long totalViews;

  @Column(name = "total_likes", nullable = false)
  private Long totalLikes;

  @Column(name = "total_comments", nullable = false)
  private Long totalComments;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}