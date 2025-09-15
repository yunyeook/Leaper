package com.ssafy.leaper.domain.advertiser.dto.response;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.file.entity.File;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserMyProfileResponse {

    private Integer advertiserId;
    private String brandName;
    private String companyName;
    private String representativeName;
    private String businessRegNo;
    private String bio;
    private String companyProfileImageUrl;
    private LocalDate openingDate;

    public static AdvertiserMyProfileResponse from(Advertiser advertiser, String companyProfileImage) {
        return AdvertiserMyProfileResponse.builder()
                .advertiserId(advertiser.getId())
                .brandName(advertiser.getBrandName())
                .companyName(advertiser.getCompanyName())
                .representativeName(advertiser.getRepresentativeName())
                .businessRegNo(advertiser.getBusinessRegNo())
                .bio(advertiser.getBio())
                .companyProfileImageUrl(companyProfileImage)
                .openingDate(advertiser.getOpeningDate())
                .build();
    }
}
