package com.ssafy.leaper.domain.content.entity;

import com.ssafy.leaper.domain.file.entity.File;
import com.ssafy.leaper.domain.platformAccount.entity.PlatformAccount;
import com.ssafy.leaper.domain.type.entity.PlatformType;
import com.ssafy.leaper.domain.type.entity.ContentType;
import jakarta.persistence.*;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.ssafy.leaper.global.converter.*;

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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_account_id", nullable = false)
    private PlatformAccount platformAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_type_id", nullable = false)
    private PlatformType platformType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_type_id", nullable = false)
    private ContentType contentType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private Integer durationSeconds;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_id")
    private File thumbnail;

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
