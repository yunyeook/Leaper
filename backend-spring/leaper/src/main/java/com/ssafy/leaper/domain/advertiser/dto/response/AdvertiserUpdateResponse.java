package com.ssafy.leaper.domain.advertiser.dto.response;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserUpdateResponse {

    private Integer advertiserId;
    private String brandName;
    private String companyName;
    private String companyProfileImageUrl;
    private String bio;
    private String representativeName;
    private String business_reg_no;
    private LocalDate opening_date;

    public static AdvertiserUpdateResponse from(Advertiser advertiser, String companyProfileImageUrl) {
        return AdvertiserUpdateResponse.builder()
                .advertiserId(advertiser.getId())
                .brandName(advertiser.getBrandName())
                .companyName(advertiser.getCompanyName())
                .companyProfileImageUrl(companyProfileImageUrl)
                .bio(advertiser.getBio())
                .representativeName(advertiser.getRepresentativeName())
                .business_reg_no(advertiser.getBusinessRegNo())
                .opening_date(advertiser.getOpeningDate())
                .build();
    }
}