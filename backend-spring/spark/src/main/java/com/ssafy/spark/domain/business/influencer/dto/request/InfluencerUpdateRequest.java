package com.ssafy.spark.domain.business.influencer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerUpdateRequest {

    private String nickname;
    private Boolean gender;
    private LocalDate birthday;
    private String email;
    private Integer profileImageId;
    private String bio;
}