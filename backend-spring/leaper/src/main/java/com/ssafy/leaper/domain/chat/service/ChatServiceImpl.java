package com.ssafy.leaper.domain.chat.service;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.advertiser.repository.AdvertiserRepository;
import com.ssafy.leaper.domain.chat.dto.request.ChatMessageSendRequest;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.chat.dto.response.ChatMessageListResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomCreateResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomListResponse;
import com.ssafy.leaper.domain.chat.dto.websocket.ChatWebSocketMessage;
import com.ssafy.leaper.domain.chat.entity.ChatMessage;
import com.ssafy.leaper.domain.chat.entity.ChatRoom;
import com.ssafy.leaper.domain.chat.entity.MessageType;
import com.ssafy.leaper.domain.chat.websocket.ChatWebSocketHandler;
import com.ssafy.leaper.global.common.entity.UserRole;
import com.ssafy.leaper.domain.chat.repository.ChatMessageRepository;
import com.ssafy.leaper.domain.chat.repository.ChatRoomRepository;
import com.ssafy.leaper.domain.influencer.entity.Influencer;
import com.ssafy.leaper.domain.influencer.repository.InfluencerRepository;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InfluencerRepository influencerRepository;
    private final AdvertiserRepository advertiserRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    @Transactional
    public ServiceResult<ChatRoomCreateResponse> createChatRoom(Integer influencerId, Integer advertiserId) {
        log.info("ChatService : createChatRoom({}, {}) 호출", influencerId, advertiserId);

        // 인플루언서와 광고주 존재 확인
        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
        if (influencer == null) {
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
        if (advertiser == null) {
            return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
        }

        // 채팅방이 있는지 먼저 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByInfluencerIdAndAdvertiserId(influencerId, advertiserId);

        ChatRoom chatRoom;
        boolean influencerJoinRequired = false;
        boolean advertiserJoinRequired = false;

        if (existingRoom.isPresent()) {
            // 기존 채팅방 존재
            log.info("기존 채팅방 존재 : {}",  existingRoom.get().getId());

            if(!existingRoom.get().getAdvertiserDeleted() && !existingRoom.get().getInfluencerDeleted()) {
                log.info("채팅방 활성화 확인 : {}",  existingRoom.get().getId());
                return ServiceResult.ok(ChatRoomCreateResponse.from(existingRoom.get()));
            }

            chatRoom = existingRoom.get();

            // 인플루언서가 삭제한 경우 복구
            if(existingRoom.get().getInfluencerDeleted()) {
                influencerJoinRequired = true;
                chatRoomRepository.restoreInfluencer(chatRoom.getId());
                log.info("인플루언서가 채팅방 복구 : {}", chatRoom.getId());
            }

            // 광고주가 삭제한 경우 복구
            if (chatRoom.getAdvertiserDeleted()) {
                advertiserJoinRequired = true;
                chatRoomRepository.restoreAdvertiser(chatRoom.getId());
                log.info("광고주가 채팅방 복구 : {}", chatRoom.getId());
            }
        }else {
            // 완전히 새로운 채팅방 생성
            influencerJoinRequired = true;
            advertiserJoinRequired = true;

            ChatRoom newRoom = ChatRoom.of(influencerId, advertiserId);
            chatRoom = chatRoomRepository.save(newRoom);
            log.info("새 채팅방 생성 : {}", chatRoom.getId());
        }

        // 인플루언서 JOIN 처리 했을 경우
        if(influencerJoinRequired) {
            // 인플루언서 JOIN 메시지 저장
            ChatMessage influencerJoinMessage = ChatMessage.of(
                    chatRoom.getId(),
                    influencerId,
                    UserRole.INFLUENCER,
                    null,
                    MessageType.JOIN
            );
            ChatMessage savedMessage = chatMessageRepository.save(influencerJoinMessage);
            log.info("인플루언서 {} 채팅방 {} JOIN 메시지 저장", influencerId, chatRoom.getId());

            // WebSocket 브로드캐스트 - CONNECT 메시지 전송
            ChatWebSocketMessage connectMessage = ChatWebSocketMessage.builder()
                    .type("CONNECT")
                    .chatRoomId(chatRoom.getId())
                    .senderId(influencerId)
                    .content(null)
                    .userRole(UserRole.INFLUENCER)
                    .messageType(MessageType.JOIN)
                    .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .build();
            chatWebSocketHandler.broadcastToChatRoom(chatRoom.getId(), connectMessage);

            // WebSocket 브로드캐스트 - JOIN 메시지 전송
            ChatWebSocketMessage joinMessage = ChatWebSocketMessage.builder()
                    .type("JOIN")
                    .chatRoomId(chatRoom.getId())
                    .senderId(influencerId)
                    .content(null)
                    .userRole(UserRole.INFLUENCER)
                    .messageType(MessageType.JOIN)
                    .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .build();
            chatWebSocketHandler.broadcastToChatRoom(chatRoom.getId(), joinMessage);
        }

        // 광고주 JOIN 처리 했을 경우
        if(advertiserJoinRequired) {
            // 광고주 JOIN 메시지 저장
            ChatMessage advertiserJoinMessage = ChatMessage.of(
                    chatRoom.getId(),
                    advertiserId,
                    UserRole.ADVERTISER,
                    null,
                    MessageType.JOIN
            );
            ChatMessage savedMessage = chatMessageRepository.save(advertiserJoinMessage);
            log.info("광고주 {} 채팅방 {} JOIN 메시지 저장", influencerId, chatRoom.getId());

            // WebSocket 브로드캐스트 - CONNECT 메시지 전송
            ChatWebSocketMessage connectMessage = ChatWebSocketMessage.builder()
                    .type("CONNECT")
                    .chatRoomId(chatRoom.getId())
                    .senderId(influencerId)
                    .content(null)
                    .userRole(UserRole.ADVERTISER)
                    .messageType(MessageType.JOIN)
                    .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .build();
            chatWebSocketHandler.broadcastToChatRoom(chatRoom.getId(), connectMessage);

            // WebSocket 브로드캐스트 - JOIN 메시지 전송
            ChatWebSocketMessage joinMessage = ChatWebSocketMessage.builder()
                    .type("JOIN")
                    .chatRoomId(chatRoom.getId())
                    .senderId(influencerId)
                    .content(null)
                    .userRole(UserRole.INFLUENCER)
                    .messageType(MessageType.JOIN)
                    .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .build();
            chatWebSocketHandler.broadcastToChatRoom(chatRoom.getId(), joinMessage);
        }

        return ServiceResult.ok(ChatRoomCreateResponse.from(chatRoom));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResult<ChatRoomListResponse> getChatRoomList(Integer currentUserId, String userRole) {
        log.info("ChatService : getChatRoomList({}, {}) 호출", currentUserId, userRole);

        UserRole currentUserRole;
        try {
            currentUserRole = UserRole.valueOf(userRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ServiceResult.fail(ErrorCode.CHAT_INVALID_USER_TYPE);
        }

        // userRole에 따라 해당하는 채팅방만 조회
        List<ChatRoom> allChatRooms;
        if (currentUserRole == UserRole.INFLUENCER) {
            allChatRooms = chatRoomRepository.findByInfluencerIdAndNotDeleted(currentUserId);
        } else if (currentUserRole == UserRole.ADVERTISER) {
            allChatRooms = chatRoomRepository.findByAdvertiserIdAndNotDeleted(currentUserId);
        } else {
            return ServiceResult.fail(ErrorCode.CHAT_INVALID_USER_TYPE);
        }

        List<ChatRoomListResponse.ChatRoomInfo> chatRoomInfos = new ArrayList<>();

        for (ChatRoom chatRoom : allChatRooms) {
            ChatRoomListResponse.ChatRoomInfo.PartnerInfo partnerInfo = null;

            if (currentUserRole == UserRole.INFLUENCER) {
                // 현재 사용자가 인플루언서인 경우 - 상대방은 광고주
                Advertiser advertiser = advertiserRepository.findById(chatRoom.getAdvertiserId()).orElse(null);
                if (advertiser != null) {
                    String profileImageUrl = null;
                    if (advertiser.getProfileImage() != null) {
                        profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(advertiser.getProfileImage().getId());
                    }
                    partnerInfo = ChatRoomListResponse.ChatRoomInfo.PartnerInfo.from(advertiser, profileImageUrl);
                }
            } else {
                // 현재 사용자가 광고주인 경우 - 상대방은 인플루언서
                Influencer influencer = influencerRepository.findById(chatRoom.getInfluencerId()).orElse(null);
                if (influencer != null) {
                    String profileImageUrl = null;
                    if (influencer.getProfileImage() != null) {
                        profileImageUrl = s3PresignedUrlService.generatePresignedDownloadUrl(influencer.getProfileImage().getId());
                    }
                    partnerInfo = ChatRoomListResponse.ChatRoomInfo.PartnerInfo.from(influencer, profileImageUrl);
                }
            }

            if (partnerInfo != null) {
                // 마지막 메시지 시간과 읽지 않은 메시지 여부 조회
                Optional<ChatMessage> lastMessage = chatMessageRepository
                        .findTopByRoomIdOrderByCreatedAtDesc(chatRoom.getId());

                // 메시지가 없는 채팅방은 목록에서 제외
                if (lastMessage.isEmpty()) {
                    continue;
                }

                boolean hasUnreadMessages = checkUnreadMessages(chatRoom, currentUserRole);
                LocalDateTime lastMessageTime = LocalDateTime.ofInstant(lastMessage.get().getCreatedAt(), java.time.ZoneId.of("Asia/Seoul"));

                ChatRoomListResponse.ChatRoomInfo chatRoomInfo = ChatRoomListResponse.ChatRoomInfo.builder()
                        .chatRoomId(chatRoom.getId())
                        .partner(partnerInfo)
                        .hasUnreadMessages(hasUnreadMessages)
                        .lastMessageTime(lastMessageTime)
                        .build();

                chatRoomInfos.add(chatRoomInfo);
            }
        }

        return ServiceResult.ok(ChatRoomListResponse.from(chatRoomInfos));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResult<ChatMessageListResponse> getChatMessages(Integer chatRoomId, String before, String after, int size) {
        log.info("ChatService : getChatMessages({}) 호출", chatRoomId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        List<ChatMessage> messages;
        boolean hasMore = false;

        if (before != null) {
            // 이전 메시지 조회 (스크롤 올릴 때)
            messages = chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(
                    chatRoomId, before, PageRequest.of(0, size));
            hasMore = messages.size() == size;
        } else if (after != null) {
            // 이후 메시지 조회 (재연결 시)
            messages = chatMessageRepository.findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(
                    chatRoomId, after, PageRequest.of(0, size));
            hasMore = false; // 최신까지 가져오므로 hasMore는 false
        } else {
            // 최초 조회 (최신 메시지부터)
            messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                    chatRoomId, PageRequest.of(0, size));
            hasMore = messages.size() == size;
        }

        return ServiceResult.ok(ChatMessageListResponse.of(
                messages.stream()
                        .map(ChatMessageListResponse.MessageInfo::from)
                        .toList(),
                hasMore
        ));
    }

    @Override
    @Transactional
    public ServiceResult<Void> sendTextMessage(Integer chatRoomId, ChatMessageSendRequest request) {
        log.info("ChatService : sendTextMessage({}) 호출", chatRoomId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        ChatMessage message = ChatMessage.of(
                chatRoomId,
                request.getSenderId(),
                request.getUserRole(),
                request.getContent(),
                request.getMessageType()
        );

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 마지막 읽은 시간 업데이트
        updateLastSeen(chatRoom, request.getUserRole());

        // WebSocket 브로드캐스트
        ChatWebSocketMessage wsMessage = ChatWebSocketMessage.builder()
                .type("CHAT")
                .chatRoomId(chatRoomId)
                .senderId(request.getSenderId())
                .content(request.getContent())
                .userRole(request.getUserRole())
                .messageType(request.getMessageType())
                .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                .build();

        chatWebSocketHandler.broadcastToChatRoom(chatRoomId, wsMessage);

        return ServiceResult.ok();
    }

    @Override
    @Transactional
    public ServiceResult<String> sendFileMessage(Integer chatRoomId, Integer senderId, String userRole, String messageType, MultipartFile file) {
        log.info("ChatService : sendFileMessage({}) 호출", chatRoomId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        try {
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            Long fileSize = file.getSize();

            // S3 presigned URL 생성 및 파일 업로드 처리
            String presignedUploadUrl = s3PresignedUrlService.generatePresignedUploadUrl(fileName, contentType);

            // 다운로드 URL 생성 (업로드 URL에서 쿼리 파라미터 제거)
            String downloadUrl = presignedUploadUrl.split("\\?")[0];

            log.info("파일 업로드 URL 생성 완료: {}", downloadUrl);

            // 파일 메시지 DB 저장
            UserRole userRoleEnum;
            try {
                userRoleEnum = UserRole.valueOf(userRole.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ServiceResult.fail(ErrorCode.CHAT_INVALID_USER_TYPE);
            }

            MessageType messageTypeEnum;
            try {
                messageTypeEnum = MessageType.valueOf(messageType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ServiceResult.fail(ErrorCode.COMMON_BAD_REQUEST);
            }

            ChatMessage fileMessage = ChatMessage.ofFile(
                    chatRoomId,
                    senderId,
                    userRoleEnum,
                    downloadUrl,
                    messageTypeEnum,
                    fileName,
                    fileSize,
                    downloadUrl
            );

            ChatMessage savedMessage = chatMessageRepository.save(fileMessage);

            // 마지막 읽은 시간 업데이트
            updateLastSeen(chatRoom, userRoleEnum);

            // WebSocket 브로드캐스트
            ChatWebSocketMessage wsMessage = ChatWebSocketMessage.builder()
                    .type("FILE")
                    .chatRoomId(chatRoomId)
                    .senderId(senderId)
                    .content(downloadUrl)
                    .userRole(userRoleEnum)
                    .messageType(messageTypeEnum)
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .fileUrl(downloadUrl)
                    .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                    .build();

            chatWebSocketHandler.broadcastToChatRoom(chatRoomId, wsMessage);

            return ServiceResult.ok(downloadUrl);
        } catch (Exception e) {
            log.error("파일 업로드 실패", e);
            return ServiceResult.fail(ErrorCode.CHAT_FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional
    public ServiceResult<Void> leaveChatRoom(Integer chatRoomId, Integer currentUserId, String userRole) {
        log.info("ChatService : leaveChatRoom({}, {}, {}) 호출", chatRoomId, currentUserId, userRole);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        UserRole currentUserRole;
        try {
            currentUserRole = UserRole.valueOf(userRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ServiceResult.fail(ErrorCode.CHAT_INVALID_USER_TYPE);
        }

        // DB에 채팅방 나간 내역 저장
        ChatMessage message = ChatMessage.of(
                chatRoomId,
                currentUserId,
                currentUserRole,
                null,
                MessageType.LEAVE
        );

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 마지막 읽은 시간 업데이트
        updateLastSeen(chatRoom, currentUserRole);

        // WebSocket 브로드캐스트 - DISCONNECT 메시지 전송
        ChatWebSocketMessage disconnectMessage = ChatWebSocketMessage.builder()
                .type("DISCONNECT")
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .userRole(currentUserRole)
                .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                .build();

        chatWebSocketHandler.broadcastToChatRoom(chatRoomId, disconnectMessage);

        // WebSocket 브로드캐스트 - LEAVE 메시지 전송
        ChatWebSocketMessage leaveMessage = ChatWebSocketMessage.builder()
                .type("LEAVE")
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .userRole(currentUserRole)
                .timestamp(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), java.time.ZoneId.of("Asia/Seoul")))
                .build();

        chatWebSocketHandler.broadcastToChatRoom(chatRoomId, leaveMessage);

        deleteByUser(chatRoom, currentUserRole);

        return ServiceResult.ok();
    }

    private void updateLastSeen(ChatRoom chatRoom, UserRole userRole) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        if (userRole == UserRole.INFLUENCER) {
            chatRoomRepository.updateInfluencerLastSeen(chatRoom.getId(), now);
        } else {
            chatRoomRepository.updateAdvertiserLastSeen(chatRoom.getId(), now);
        }
    }

    private void deleteByUser(ChatRoom chatRoom, UserRole userRole) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        if (userRole == UserRole.INFLUENCER) {
            chatRoomRepository.deleteByInfluencer(chatRoom.getId(), now);
        } else {
            chatRoomRepository.deleteByAdvertiser(chatRoom.getId(), now);
        }
    }

    private boolean checkUnreadMessages(ChatRoom chatRoom, UserRole userRole) {
        LocalDateTime lastSeen = (userRole == UserRole.INFLUENCER)
                ? chatRoom.getInfluencerLastSeen()
                : chatRoom.getAdvertiserLastSeen();

        if (lastSeen == null) {
            return true; // 한 번도 읽지 않았으면 읽지 않은 메시지가 있음
        }

        Instant lastSeenInstant = lastSeen.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();

        log.info("lastSeen : {}", lastSeenInstant);

        // 상대방이 보낸 메시지만 확인 (내 UserRole이 아닌 메시지만 확인)
        return chatMessageRepository.existsByRoomIdAndCreatedAtAfterAndUserRoleNot(chatRoom.getId(), lastSeenInstant, userRole);
    }
}