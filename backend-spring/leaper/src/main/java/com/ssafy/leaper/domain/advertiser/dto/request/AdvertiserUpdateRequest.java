package com.ssafy.leaper.domain.advertiser.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserUpdateRequest {

    private MultipartFile companyProfileImage;

    @Size(max = 61, message = "브랜드명은 61자 이하여야 합니다.")
    private String brandName;

    @Size(max = 401, message = "자기소개는 401자 이하여야 합니다.")
    private String bio;

    @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 10자리 숫자여야 합니다.")
    private String businessRegNo;

    @Size(max = 100, message = "대표자명은 100자 이하여야 합니다.")
    private String representativeName;

    private LocalDate openingDate;
}