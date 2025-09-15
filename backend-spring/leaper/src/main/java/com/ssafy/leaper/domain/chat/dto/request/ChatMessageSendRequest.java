package com.ssafy.leaper.domain.chat.dto.request;

import com.ssafy.leaper.domain.chat.entity.MessageType;
import com.ssafy.leaper.global.common.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendRequest {
    @NotNull(message = "발신자 ID는 필수입니다.")
    private Integer senderId;
    
    @NotBlank(message = "메시지는 필수입니다.")
    private String content;

    @NotNull(message = "유저 타입은 필수입니다.")
    private UserRole userRole;
    
    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType messageType;
}
