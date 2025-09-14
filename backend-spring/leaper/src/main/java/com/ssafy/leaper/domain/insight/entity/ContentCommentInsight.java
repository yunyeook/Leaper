// ContentCommentInsight.java
package com.ssafy.leaper.domain.insight.entity;

import com.ssafy.leaper.domain.content.entity.Content;
import com.ssafy.leaper.global.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "content_comment_insight")
public class ContentCommentInsight {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "content_comment_insight_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @Convert(converter = StringListJsonConverter.class)
  @Column(name = "positive_comment_json", columnDefinition = "TEXT")
  private List<String> positiveComments;

  @Convert(converter = StringListJsonConverter.class)
  @Column(name = "negative_comment_json", columnDefinition = "TEXT")
  private List<String> negativeComments;

  @Column(precision = 5, scale = 2)
  private BigDecimal likeScore;

  @Column(columnDefinition = "TEXT")
  private String summaryText;

  @Convert(converter = StringListJsonConverter.class)
  @Column(name = "keywords_json", columnDefinition = "TEXT")
  private List<String> keywords;

  private String modelVersion;

  @Column( nullable = false)
  private LocalDate snapshotDate;

  @CreatedDate
  private LocalDateTime createdAt;
}