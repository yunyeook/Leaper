package com.ssafy.leaper.domain.advertiser.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvertiserSignupRequest {

    @NotBlank(message = "로그인 아이디는 필수입니다")
    @Size(max = 21, message = "로그인 아이디는 21자 이내여야 합니다")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    private MultipartFile companyProfileImage;

    @NotBlank(message = "브랜드명은 필수입니다")
    @Size(max = 61, message = "브랜드명은 61자 이내여야 합니다")
    private String brandName;

    @Size(max = 401, message = "소개는 401자 이내여야 합니다")
    private String bio;

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