package com.ssafy.leaper.domain.advertiser.dto.response;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserPublicProfileResponse {

    private String brandName;
    private String companyName;
    private String companyProfileImageUrl;
    private String representativeName;

    public static AdvertiserPublicProfileResponse from(Advertiser advertiser, String companyProfileImageUrl) {
        return AdvertiserPublicProfileResponse.builder()
                .brandName(advertiser.getBrandName())
                .companyName(advertiser.getCompanyName())
                .companyProfileImageUrl(companyProfileImageUrl)
                .representativeName(advertiser.getRepresentativeName())
                .build();
    }
}