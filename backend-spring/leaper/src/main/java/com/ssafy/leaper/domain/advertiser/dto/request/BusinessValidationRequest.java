package com.ssafy.leaper.domain.advertiser.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessValidationRequest {

    private List<Business> businesses;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Business {

        @JsonProperty("b_no")
        private String businessNumber;

        @JsonProperty("start_dt")
        private String startDate;

        @JsonProperty("p_nm")
        private String representativeName;

        @JsonProperty("p_nm2")
        private String representativeName2;

        @JsonProperty("b_nm")
        private String businessName;

        @JsonProperty("corp_no")
        private String corporationNumber;

        @JsonProperty("b_sector")
        private String businessSector;

        @JsonProperty("b_type")
        private String businessType;

        @JsonProperty("b_adr")
        private String businessAddress;
    }
}