package com.ssafy.leaper.domain.platformAccount.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlatformAccountCreateRequest {

    @NotBlank(message = "externalAccountId는 필수입니다")
    @Size(max = 320, message = "externalAccountId는 320자를 초과할 수 없습니다")
    private String externalAccountId;

    @NotBlank(message = "platformTypeId는 필수입니다")
    @Size(max = 31, message = "platformTypeId는 31자를 초과할 수 없습니다")
    private String platformTypeId;

    @NotBlank(message = "accountNickname은 필수입니다")
    @Size(max = 301, message = "accountNickname은 301자를 초과할 수 없습니다")
    private String accountNickname;

    @Size(max = 500, message = "accountUrl은 500자를 초과할 수 없습니다")
    private String accountUrl;

    @Size(max = 500, message = "accountProfileImageUrl은 500자를 초과할 수 없습니다")
    private String accountProfileImageUrl;

    @NotNull(message = "categoryTypeId는 필수입니다")
    private Short categoryTypeId;
}