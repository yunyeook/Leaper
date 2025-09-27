package com.ssafy.spark.domain.business.content.entity;

import com.ssafy.spark.domain.business.platformAccount.entity.PlatformAccount;
import com.ssafy.spark.domain.business.type.entity.ContentType;
import com.ssafy.spark.domain.business.type.entity.PlatformType;
import com.ssafy.spark.global.converter.StringListJsonConverter;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_account_id", nullable = false)
    private PlatformAccount platformAccount; // id integer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_type_id", nullable = false)
    private PlatformType platformType; // id string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_type_id", nullable = false)
    private ContentType contentType; // id string

    @Column(name = "external_content_id", nullable = false, length = 320)
    private String externalContentId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private Integer durationSeconds;

    private Integer thumbnailId;

    @Column(nullable = false, length = 500)
    private String contentUrl;

    private LocalDateTime publishedAt;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> tagsJson;

    @Column(nullable = false)
    private BigInteger totalViews;

    @Column(nullable = false)
    private BigInteger totalLikes;

    @Column( nullable = false)
    private BigInteger totalComments;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDate snapshotDate;

}
