package com.ssafy.leaper.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserLoginRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다")
    @Size(max = 21, message = "로그인 아이디는 21자 이내여야 합니다")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}