package com.ssafy.leaper.domain.influencer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class InfluencerUpdateRequest {

    private MultipartFile profileImage;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 61, message = "닉네임은 61자 이하여야 합니다.")
    private String nickname;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 320, message = "이메일은 320자 이하여야 합니다.")
    private String email;

    @Size(max = 401, message = "소개는 401자 이하여야 합니다.")
    private String bio;

    @NotNull(message = "생년월일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank(message = "성별은 필수입니다.")
    private String gender;

    public Boolean getGenderAsBoolean() {
        return "FEMALE".equals(gender);
    }
}