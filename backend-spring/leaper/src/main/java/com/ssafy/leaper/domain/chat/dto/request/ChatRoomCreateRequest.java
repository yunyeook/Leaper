package com.ssafy.leaper.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {
    @NotNull(message = "인플루언서 ID는 필수입니다.")
    private Integer influencerId;
    
    @NotNull(message = "광고주 ID는 필수입니다.")
    private Integer advertiserId;
}