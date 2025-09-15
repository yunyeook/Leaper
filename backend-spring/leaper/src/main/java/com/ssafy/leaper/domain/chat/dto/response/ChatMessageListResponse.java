package com.ssafy.leaper.domain.chat.dto.response;

import com.ssafy.leaper.domain.chat.entity.ChatMessage;
import com.ssafy.leaper.domain.chat.entity.MessageType;
import com.ssafy.leaper.global.common.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatMessageListResponse {
    private List<MessageInfo> messages;
    private Boolean hasMore;
    
    public static ChatMessageListResponse of(List<MessageInfo> messages, Boolean hasMore) {
        return ChatMessageListResponse.builder()
                .messages(messages)
                .hasMore(hasMore)
                .build();
    }
    
    @Getter
    @Builder
    public static class MessageInfo {
        private String messageId;
        private Integer senderId;
        private UserRole userRole;
        private String content;
        private MessageType messageType;
        private LocalDateTime createdAt;
        private String fileName;
        private Long fileSize;
        
        public static MessageInfo from(ChatMessage chatMessage) {
            return MessageInfo.builder()
                    .messageId(chatMessage.getId())
                    .senderId(chatMessage.getSenderId())
                    .userRole(chatMessage.getUserRole())
                    .content(chatMessage.getContent())
                    .messageType(chatMessage.getMessageType())
                    .createdAt(LocalDateTime.ofInstant(chatMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .fileName(chatMessage.getFileName())
                    .fileSize(chatMessage.getFileSize())
                    .build();
        }
    }
}