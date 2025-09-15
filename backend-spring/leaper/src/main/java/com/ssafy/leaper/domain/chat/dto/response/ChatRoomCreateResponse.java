package com.ssafy.leaper.domain.chat.dto.response;

import com.ssafy.leaper.domain.chat.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomCreateResponse {
    private Integer chatRoomId;
    
    public static ChatRoomCreateResponse from(ChatRoom chatRoom) {
        return ChatRoomCreateResponse.builder()
                .chatRoomId(chatRoom.getId())
                .build();
    }
}