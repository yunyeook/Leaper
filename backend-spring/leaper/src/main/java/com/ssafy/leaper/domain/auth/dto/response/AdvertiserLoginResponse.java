package com.ssafy.leaper.domain.auth.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserLoginResponse {

    private String advertiserId;
    private String accessToken;
}