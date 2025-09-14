package com.ssafy.leaper.domain.chat.dto.websocket;

import com.ssafy.leaper.domain.chat.entity.MessageType;
import com.ssafy.leaper.global.common.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatWebSocketMessage {
    private String type;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private UserRole userRole;
    private MessageType messageType;
    private String messageId;
    private LocalDateTime timestamp;

    // 파일 메시지용 필드들
    private String fileName;
    private Long fileSize;
    private String fileUrl;
}