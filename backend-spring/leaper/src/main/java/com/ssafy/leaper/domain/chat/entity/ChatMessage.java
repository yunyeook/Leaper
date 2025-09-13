package com.ssafy.leaper.domain.chat.entity;
import com.ssafy.leaper.global.common.entity.UserRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessage {

    @Id
    private String id;

    private Long roomId;
    private Long senderId;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @CreatedDate
    private Instant createdAt;

    // 파일 관련 필드 (FILE, IMAGE 타입일 때 사용)
    private String fileName;

    private Long fileSize;

    private String fileUrl;

    // 정적 팩토리 메서드
    public static ChatMessage of(Long roomId, Long senderId, UserRole userRole, String content, MessageType messageType) {
        return ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .userRole(userRole)
                .content(content)
                .messageType(messageType)
                .build();
    }

    public static ChatMessage ofFile(Long roomId, Long senderId, UserRole userRole, String content, MessageType messageType,
                                     String fileName, Long fileSize, String fileUrl) {
        return ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .userRole(userRole)
                .content(content)
                .messageType(messageType)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileUrl(fileUrl)
                .build();
    }
}