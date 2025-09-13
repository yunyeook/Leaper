package com.ssafy.leaper.domain.chat.controller;

import com.ssafy.leaper.domain.chat.dto.request.ChatMessageSendRequest;
import com.ssafy.leaper.domain.chat.dto.response.ChatMessageListResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomCreateResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomListResponse;
import com.ssafy.leaper.domain.chat.service.ChatService;
import com.ssafy.leaper.global.common.controller.BaseController;
import com.ssafy.leaper.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/chatRoom")
@RequiredArgsConstructor
@Tag(name = "ChatRoom", description = "채팅방 관리 API")
public class ChatRoomController implements BaseController {

    private final ChatService chatService;

    /**
     * 채팅방 생성
     * POST /api/v1/chatRoom?influencer={influencerId}&advertiser={advertiserId}
     */
    @PostMapping
    @Operation(summary = "채팅방 생성", description = "인플루언서와 광고주 간의 새로운 채팅방을 생성합니다.")
    public ResponseEntity<ApiResponse<ChatRoomCreateResponse>> createChatRoom(
            @RequestParam("influencer") Long influencerId,
            @RequestParam("advertiser") Long advertiserId) {

        return handle(chatService.createChatRoom(influencerId, advertiserId));
    }

    /**
     * 채팅방 목록 조회
     * GET /api/v1/chatRoom
     */
    @GetMapping
    @Operation(summary = "채팅방 목록 조회", description = "현재 사용자의 채팅방 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> getChatRoomList(
            // TODO: Authentication에서 현재 사용자 정보 추출
            @RequestHeader("Authorization") String authorization) {

        // 임시로 하드코딩 (추후 인증 구현 시 수정)
        Long currentUserId = 1L; // TODO: JWT에서 추출
        String userRole = "ADVERTISER"; // TODO: JWT에서 추출

        return handle(chatService.getChatRoomList(currentUserId, userRole));
    }

    /**
     * 채팅 메시지 조회
     * GET /api/v1/chatRoom/{chatRoomId}/message
     */
    @GetMapping("/{chatRoomId}/message")
    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 메시지 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> getChatMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "50") int size) {
        
        return handle(chatService.getChatMessages(chatRoomId, before, after, size));
    }

    /**
     * 텍스트 메시지 전송
     * POST /api/v1/chatRoom/{chatRoomId}/message
     */
    @PostMapping(value = "/{chatRoomId}/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "텍스트 메시지 전송", description = "채팅방에 텍스트 메시지를 전송합니다." +
            "(Swagger에서 텍스트/파일 api를 정상적으로 분리해서 제공하지 못하고 있으니 Notion에서 확인 바랍니다.")

    public ResponseEntity<ApiResponse<Void>> sendTextMessage(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        
        return handle(chatService.sendTextMessage(chatRoomId, request));
    }

    /**
     * 파일/이미지 메시지 전송
     * POST /api/v1/chatRoom/{chatRoomId}/message
     */
    @PostMapping(value = "/{chatRoomId}/message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일/이미지 메시지 전송", description = "채팅방에 파일 또는 이미지 메시지를 전송합니다.")
    public ResponseEntity<ApiResponse<Void>> sendFileMessage(
            @PathVariable Long chatRoomId,
            @RequestParam Long senderId,
            @RequestParam String userRole,
            @RequestParam String messageType,
            @RequestParam MultipartFile file) {
        
        return handle(chatService.sendFileMessage(chatRoomId, senderId, userRole, messageType, file));
    }

    /**
     * 채팅방 나가기
     * DELETE /api/v1/chatRoom/{chatRoomId}
     */
    @DeleteMapping("/{chatRoomId}")
    @Operation(summary = "채팅방 나가기", description = "사용자가 채팅방에서 나갑니다.")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @PathVariable Long chatRoomId,
            // TODO: Authentication에서 현재 사용자 정보 추출
            @RequestHeader("Authorization") String authorization) {

        // 임시로 하드코딩 (추후 인증 구현 시 수정)
        Long currentUserId = 1L; // TODO: JWT에서 추출
        String userRole = "ADVERTISER"; // TODO: JWT에서 추출

        return handle(chatService.leaveChatRoom(chatRoomId, currentUserId, userRole));
    }
}