package com.ssafy.leaper.domain.chat.service;

import com.ssafy.leaper.domain.chat.dto.request.ChatMessageSendRequest;
import com.ssafy.leaper.domain.chat.dto.response.ChatMessageListResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomCreateResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomListResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.common.entity.UserRole;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    /**
     * 채팅방 생성
     */
    ServiceResult<ChatRoomCreateResponse> createChatRoom(Integer influencerId, Integer advertiserId);

    /**
     * 채팅방 목록 조회
     */
    ServiceResult<ChatRoomListResponse> getChatRoomList(Integer currentUserId, String userRole);

    /**
     * 채팅 메시지 조회
     */
    ServiceResult<ChatMessageListResponse> getChatMessages(Integer chatRoomId, String before, String after, int size, String userRole);

    /**
     * 텍스트 메시지 전송
     */
    ServiceResult<Void> sendTextMessage(Integer chatRoomId, ChatMessageSendRequest request);

    /**
     * 파일/이미지 메시지 전송 (파일 업로드만 처리, 다운로드 URL 반환)
     */
    ServiceResult<String> sendFileMessage(Integer chatRoomId, Integer senderId, String userRole, String messageType, MultipartFile file);

    /**
     * 채팅방 나가기
     */
    ServiceResult<Void> leaveChatRoom(Integer chatRoomId, Integer currentUserId, String userRole);

    /**
     * 마지막 접속 시간 갱신
     */
    void updateLastSeen(Integer chatRoomId, UserRole userRole);
}