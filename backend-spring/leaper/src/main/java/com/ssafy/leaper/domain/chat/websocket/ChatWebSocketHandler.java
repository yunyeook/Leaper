package com.ssafy.leaper.domain.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.leaper.domain.chat.dto.request.ChatMessageSendRequest;
import com.ssafy.leaper.domain.chat.dto.websocket.ChatWebSocketMessage;
import com.ssafy.leaper.domain.chat.entity.ChatMessage;
import com.ssafy.leaper.domain.chat.repository.ChatMessageRepository;
import com.ssafy.leaper.domain.chat.service.ChatService;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.global.common.entity.UserRole;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final S3PresignedUrlService s3PresignedUrlService;

    // 채팅방별로 연결된 세션들을 관리
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
    // 세션별로 사용자 정보를 관리
    private final Map<String, UserSessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    // 소켓 연결 성공(채팅방 참여 전) - log용
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 연결 성공: {}", session.getId());
    }

    // 채팅방 (JOIN/CHAT/LEAVE) 핸들러 - 분기 처리
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatWebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), ChatWebSocketMessage.class);
        if (wsMessage == null) {
            sendErrorCode(session, ErrorCode.COMMON_INVALID_FORMAT);
            return;
        }

        log.info("WebSocket 메시지 수신: {}", wsMessage.getType());

        ServiceResult<Void> result = switch (wsMessage.getType()) {
            case "JOIN" -> handleJoinMessage(session, wsMessage);
            case "CHAT" -> handleChatMessage(session, wsMessage);
            case "FILE" -> handleFileMessage(session, wsMessage);
            case "LEAVE" -> handleLeaveMessage(session, wsMessage);
            default -> {
                log.warn("알 수 없는 메시지 타입: {}", wsMessage.getType());
                yield ServiceResult.fail(ErrorCode.COMMON_INVALID_FORMAT);
            }
        };

        if (!result.success()) {
            sendErrorCode(session, result.code());
        }
    }

    // 세선 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket 연결 종료: {}", session.getId());

        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo != null) {
            // 채팅방에서 세션 제거
            Long chatRoomId = sessionInfo.getChatRoomId();
            if (chatRoomId != null) {
                CopyOnWriteArraySet<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        chatRoomSessions.remove(chatRoomId);
                    }
                }
            }
            sessionInfoMap.remove(session.getId());
        }
    }

    // 채팅방 접속 핸들러(JOIN)
    private ServiceResult<Void> handleJoinMessage(WebSocketSession session, ChatWebSocketMessage message) {
        Long chatRoomId = message.getChatRoomId();
        Long userId = message.getSenderId();
        UserRole userRole = message.getUserRole();

        if (chatRoomId == null || userId == null || userRole == null) {
            return ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
        }

        // 세션 정보 저장
        sessionInfoMap.put(session.getId(), new UserSessionInfo(chatRoomId, userId, userRole));

        // 채팅방에 세션 추가
        chatRoomSessions.computeIfAbsent(chatRoomId, k -> new CopyOnWriteArraySet<>()).add(session);

        log.info("사용자 {}이 채팅방 {}에 참여했습니다.", userId, chatRoomId);

        // JOIN 성공 응답
        ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                .type("JOIN_SUCCESS")
                .chatRoomId(chatRoomId)
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(session, response);
        return ServiceResult.ok();
    }

    // 채팅방 메시지 핸들러(CHAT)
    private ServiceResult<Void> handleChatMessage(WebSocketSession session, ChatWebSocketMessage message) {
        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
        }

        // 메시지 저장
        ChatMessageSendRequest request = new ChatMessageSendRequest(
                message.getSenderId(),
                message.getContent(),
                message.getUserRole(),
                message.getMessageType()
        );

        ServiceResult<Void> result = chatService.sendTextMessage(message.getChatRoomId(), request);

        if (!result.success()) {
            return ServiceResult.fail(result.code());
        }

        // 같은 채팅방의 모든 세션에 메시지 브로드캐스트
        broadcastToChatRoom(message.getChatRoomId(), message);
        return ServiceResult.ok();
    }

    // 파일 메시지 핸들러(FILE)
    private ServiceResult<Void> handleFileMessage(WebSocketSession session, ChatWebSocketMessage message) {
        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        // 파일 정보 검증
        if (message.getFileName() == null || message.getFileSize() == null || message.getFileUrl() == null) {
            return ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
        }

        // 파일 메시지 저장 (직접 ChatMessage 저장)
        try {
            ChatMessage chatMessage = ChatMessage.ofFile(
                    message.getChatRoomId(),
                    message.getSenderId(),
                    message.getUserRole(),
                    message.getFileUrl(),
                    message.getMessageType(),
                    message.getFileName(),
                    message.getFileSize(),
                    message.getFileUrl()
            );

            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            log.error("파일 메시지 저장 실패", e);
            return ServiceResult.fail(ErrorCode.CHAT_FILE_UPLOAD_FAILED);
        }

        // 파일 정보를 포함한 메시지 브로드캐스트
        ChatWebSocketMessage fileMessage = ChatWebSocketMessage.builder()
                .type("FILE")
                .chatRoomId(message.getChatRoomId())
                .senderId(message.getSenderId())
                .content(message.getFileUrl())
                .userRole(message.getUserRole())
                .messageType(message.getMessageType())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .fileUrl(message.getFileUrl())
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToChatRoom(message.getChatRoomId(), fileMessage);
        return ServiceResult.ok();
    }

    // 채팅방 나가기 핸들러(LEAVE)
    private ServiceResult<Void> handleLeaveMessage(WebSocketSession session, ChatWebSocketMessage message) {
        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo == null) {
            return ServiceResult.ok();
        }

        Long chatRoomId = sessionInfo.getChatRoomId();

        // 다른 사용자들에게 나가기 메시지 브로드캐스트 (세션 제거 전에 실행)
        ChatWebSocketMessage leaveMessage = ChatWebSocketMessage.builder()
                .type("LEAVE")
                .chatRoomId(chatRoomId)
                .senderId(sessionInfo.getUserId())
                .userRole(sessionInfo.getUserRole())
                .content(null)
                .messageType(null)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToChatRoom(chatRoomId, leaveMessage);

        // 채팅방에서 세션 제거
        CopyOnWriteArraySet<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                chatRoomSessions.remove(chatRoomId);
            }
        }

        sessionInfoMap.remove(session.getId());
        log.info("사용자 {}이 채팅방 {}에서 나갔습니다.", sessionInfo.getUserId(), chatRoomId);
        return ServiceResult.ok();
    }

    // 채팅 메시지 브로드캐스팅 - 채팅방 내 참여자들에게 메시지 전송
    private void broadcastToChatRoom(Long chatRoomId, ChatWebSocketMessage message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = chatRoomSessions.get(chatRoomId);
        if (sessions == null) {
            return;
        }

        message.setTimestamp(LocalDateTime.now());

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendMessage(session, message);
            } else {
                sessions.remove(session);
            }
        }
    }

    // 메시지 전송(유틸 메서드) - 각 세션으로 메시지 전송
    private void sendMessage(WebSocketSession session, ChatWebSocketMessage message) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (Exception e) {
            log.error("메시지 전송 실패: {}", session.getId(), e);
        }
    }

    private void sendErrorCode(WebSocketSession session, ErrorCode errorCode) {
        ChatWebSocketMessage error = ChatWebSocketMessage.builder()
                .type("ERROR")
                .content(errorCode != null ? errorCode.getCode() : "UNKNOWN_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(session, error);
    }

    @Getter
    @AllArgsConstructor
    private static class UserSessionInfo {
        private final Long chatRoomId;
        private final Long userId;
        private final UserRole userRole;
    }
}