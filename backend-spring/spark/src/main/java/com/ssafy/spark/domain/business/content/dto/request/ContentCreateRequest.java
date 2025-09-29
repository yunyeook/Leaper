package com.ssafy.spark.domain.business.content.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreateRequest {

    private Integer platformAccountId;
    private String platformTypeId;
    private String contentTypeId;
    private String title;
    private String description;
    private Integer durationSeconds;
    private Integer thumbnailId;
    private String contentUrl;
    private LocalDateTime publishedAt;
    private List<String> tagsJson;
    private BigInteger totalViews;
    private BigInteger totalLikes;
    private BigInteger totalComments;
    private LocalDate snapshotDate;
}