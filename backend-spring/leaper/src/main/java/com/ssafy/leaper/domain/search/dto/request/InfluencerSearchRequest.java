package com.ssafy.leaper.domain.search.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerSearchRequest {
    private String keyword;
    private String platform;
    private String category;
    private String contentType;

    @Min(value = 0, message = "최소 팔로워 수는 0 이상이어야 합니다.")
    private Integer minFollowers;

    @Min(value = 0, message = "최대 팔로워 수는 0 이상이어야 합니다.")
    private Integer maxFollowers;
}