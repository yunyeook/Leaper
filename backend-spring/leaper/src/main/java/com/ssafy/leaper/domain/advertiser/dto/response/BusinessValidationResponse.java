package com.ssafy.leaper.domain.advertiser.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessValidationResponse {

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("request_cnt")
    private Integer requestCount;

    @JsonProperty("valid_cnt")
    private Integer validCount;

    private List<BusinessValidationResult> data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessValidationResult {

        @JsonProperty("b_no")
        private String businessNumber;

        @JsonProperty("valid")
        private String valid;

        @JsonProperty("valid_msg")
        private String validMessage;

        @JsonProperty("request_param")
        private RequestParam requestParam;

        @JsonProperty("status")
        private Status status;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RequestParam {
            @JsonProperty("b_no")
            private String businessNumber;

            @JsonProperty("start_dt")
            private String startDate;

            @JsonProperty("p_nm")
            private String representativeName;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Status {
            @JsonProperty("b_no")
            private String businessNumberStatus;

            @JsonProperty("b_stt")
            private String businessStatus;

            @JsonProperty("b_stt_cd")
            private String businessStatusCode;

            @JsonProperty("tax_type")
            private String taxType;

            @JsonProperty("tax_type_cd")
            private String taxTypeCode;

            @JsonProperty("end_dt")
            private String endDate;

            @JsonProperty("utcc_yn")
            private String utccYn;

            @JsonProperty("tax_type_change_dt")
            private String taxTypeChangeDate;

            @JsonProperty("invoice_apply_dt")
            private String invoiceApplyDate;
        }
    }
}