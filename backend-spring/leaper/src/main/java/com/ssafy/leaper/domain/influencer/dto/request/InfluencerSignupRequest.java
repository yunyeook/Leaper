package com.ssafy.leaper.domain.influencer.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfluencerSignupRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 61, message = "닉네임은 61자 이내여야 합니다")
    private String nickname;

    @Size(max = 401, message = "자기소개는 401자 이내여야 합니다")
    private String bio;

    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE이어야 합니다")
    private String gender;

    @NotBlank(message = "소셜 제공자 타입은 필수입니다")
    @Pattern(regexp = "^(GOOGLE|NAVER|KAKAO)$", message = "지원되지 않는 소셜 제공자입니다")
    private String providerTypeId;

    @NotBlank(message = "소셜 제공자 고유 ID는 필수입니다")
    @Size(max = 31, message = "소셜 제공자 고유 ID는 31자 이내여야 합니다")
    private String providerMemberId;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 320, message = "이메일은 320자 이내여야 합니다")
    private String email;

    private MultipartFile profileImage;

    public Integer getGenderAsInteger() {
        return "MALE".equals(gender) ? 0 : 1;
    }
}