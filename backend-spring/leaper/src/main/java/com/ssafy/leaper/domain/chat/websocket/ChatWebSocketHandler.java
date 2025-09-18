package com.ssafy.leaper.domain.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.leaper.domain.chat.dto.websocket.ChatWebSocketMessage;
import com.ssafy.leaper.domain.chat.repository.ChatRoomRepository;
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

    private final ObjectMapper objectMapper;

    // 채팅방별로 연결된 세션들을 관리
    private final Map<Integer, CopyOnWriteArraySet<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();
    // 세션별로 사용자 정보를 관리
    private final Map<String, UserSessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    // 소켓 연결 성공(채팅방 참여 전) - log용
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket 연결 성공: {}", session.getId());
    }

    // 세션 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket 연결 종료: {}", session.getId());

        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo != null) {
            // 채팅방에서 세션 제거
            Integer chatRoomId = sessionInfo.getChatRoomId();
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
            log.info("사용자 {}이 채팅방 {}에서 나갔습니다.", sessionInfo.getUserId(), chatRoomId);
        }
    }

    // 채팅방 (CONNECT/DISCONNECT) 핸들러 : 세션 생성/제거 관리
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatWebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), ChatWebSocketMessage.class);
        if (wsMessage == null) {
            sendErrorCode(session, ErrorCode.COMMON_INVALID_FORMAT);
            return;
        }

        log.info("WebSocket 메시지 수신: {}", wsMessage.getType());
        ServiceResult<Void> result = switch (wsMessage.getType()) {
            case "CONNECT" -> handleConnectMessage(session, wsMessage);
            case "DISCONNECT" -> handleDisconnectMessage(session);
            case "CHAT", "FILE" -> {
                log.warn("CHAT/FILE 메시지는 REST API를 사용해주세요: {}", wsMessage.getType());
                yield ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
            }
            default -> {
                log.warn("알 수 없는 메시지 타입: {}", wsMessage.getType());
                yield ServiceResult.fail(ErrorCode.COMMON_INVALID_FORMAT);
            }
        };

        if (!result.success()) {
            sendErrorCode(session, result.code());
        }
    }

    // 채팅방 접속 핸들러(CONNECT)
    private ServiceResult<Void> handleConnectMessage(WebSocketSession session, ChatWebSocketMessage message) {
        Integer chatRoomId = message.getChatRoomId();
        Integer userId = message.getSenderId();
        UserRole userRole = message.getUserRole();

        if (chatRoomId == null || userId == null || userRole == null) {
            return ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
        }

        // 세션 정보 저장
        sessionInfoMap.put(session.getId(), new UserSessionInfo(chatRoomId, userId, userRole));

        // 채팅방에 세션 추가
        chatRoomSessions.computeIfAbsent(chatRoomId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("사용자 {}이 채팅방 {}에 참여했습니다.", userId, chatRoomId);

        // JOIN 성공 응답 (본인에게)
        ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                .type("CONNECT_SUCCESS")
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .userRole(userRole)
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(session, response);

        // 다른 사용자들에게 CONNECT 메시지 브로드캐스트
        ChatWebSocketMessage connectBroadcast = ChatWebSocketMessage.builder()
                .type("CONNECT")
                .chatRoomId(chatRoomId)
                .senderId(userId)
                .userRole(userRole)
                .content(null)
                .messageType(null)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToChatRoom(chatRoomId, connectBroadcast);

        return ServiceResult.ok();
    }

    // 채팅방 나가기 핸들러(DISCONNECT)
    private ServiceResult<Void> handleDisconnectMessage(WebSocketSession session) {
        UserSessionInfo sessionInfo = sessionInfoMap.get(session.getId());
        if (sessionInfo == null) {
            return ServiceResult.ok();
        }

        Integer chatRoomId = sessionInfo.getChatRoomId();
        Integer senderId = sessionInfo.getUserId();
        UserRole userRole = sessionInfo.getUserRole();

        // 다른 사용자들에게 DISCONNECT 메시지 브로드캐스트 (세션 제거 전에 실행)
        ChatWebSocketMessage disconnectMessage = ChatWebSocketMessage.builder()
                .type("DISCONNECT")
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .userRole(userRole)
                .content(null)
                .messageType(null)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToChatRoom(chatRoomId, disconnectMessage);

        return ServiceResult.ok();
    }

    // 채팅 메시지 브로드캐스팅 - 채팅방 내 참여자들에게 메시지 전송
    public void broadcastToChatRoom(Integer chatRoomId, ChatWebSocketMessage message) {
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
        private final Integer chatRoomId;
        private final Integer userId;
        private final UserRole userRole;
    }
}