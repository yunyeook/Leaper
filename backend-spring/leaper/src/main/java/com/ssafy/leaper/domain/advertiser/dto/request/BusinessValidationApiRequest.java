package com.ssafy.leaper.domain.advertiser.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessValidationApiRequest {

    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 10자리 숫자여야 합니다")
    private String businessRegNo;

    @NotBlank(message = "대표자명은 필수입니다")
    @Size(max = 100, message = "대표자명은 100자 이내여야 합니다")
    private String representativeName;

    @NotNull(message = "개업일은 필수입니다")
    @PastOrPresent(message = "개업일은 현재 또는 과거 날짜여야 합니다")
    private LocalDate openingDate;
}