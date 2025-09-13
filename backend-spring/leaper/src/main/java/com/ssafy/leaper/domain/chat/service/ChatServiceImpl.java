package com.ssafy.leaper.domain.chat.service;

import com.ssafy.leaper.domain.advertiser.entity.Advertiser;
import com.ssafy.leaper.domain.advertiser.repository.AdvertiserRepository;
import com.ssafy.leaper.domain.chat.dto.request.ChatMessageSendRequest;
import com.ssafy.leaper.domain.file.service.S3PresignedUrlService;
import com.ssafy.leaper.domain.chat.dto.response.ChatMessageListResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomCreateResponse;
import com.ssafy.leaper.domain.chat.dto.response.ChatRoomListResponse;
import com.ssafy.leaper.domain.chat.entity.ChatMessage;
import com.ssafy.leaper.domain.chat.entity.ChatRoom;
import com.ssafy.leaper.domain.chat.entity.MessageType;
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

    @Override
    @Transactional
    public ServiceResult<ChatRoomCreateResponse> createChatRoom(Long influencerId, Long advertiserId) {
        log.info("ChatRoomService : createChatRoom({}, {}) 호출", influencerId, advertiserId);

        // 인플루언서와 광고주 존재 확인
        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);
        if (influencer == null) {
            return ServiceResult.fail(ErrorCode.INFLUENCER_NOT_FOUND);
        }

        Advertiser advertiser = advertiserRepository.findById(advertiserId).orElse(null);
        if (advertiser == null) {
            return ServiceResult.fail(ErrorCode.ADVERTISER_NOT_FOUND);
        }

        // 활성화된 채팅방이 있는지 먼저 확인
        Optional<ChatRoom> activeRoom = chatRoomRepository
                .findByInfluencerIdAndAdvertiserIdAndNotDeleted(influencerId, advertiserId);

        if (activeRoom.isPresent()) {
            log.info("활성화된 채팅방 존재 : {}",  activeRoom.get().getId());
            return ServiceResult.ok(ChatRoomCreateResponse.from(activeRoom.get()));
        }

        // 삭제된 채팅방이 있는지 확인 (삭제된 것 포함)
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByInfluencerIdAndAdvertiserId(influencerId, advertiserId);

        if (existingRoom.isPresent()) {
            ChatRoom chatRoom = existingRoom.get();
            log.info("삭제된 채팅방 복구 : {}", chatRoom.getId());

            // 인플루언서가 삭제한 경우 복구
            if (chatRoom.getInfluencerDeleted()) {
                chatRoomRepository.restoreInfluencer(chatRoom.getId());
            }

            // 광고주가 삭제한 경우 복구
            if (chatRoom.getAdvertiserDeleted()) {
                chatRoomRepository.restoreAdvertiser(chatRoom.getId());
            }

            // 복구된 채팅방 다시 조회해서 반환
            ChatRoom restoredRoom = chatRoomRepository.findById(chatRoom.getId()).get();
            return ServiceResult.ok(ChatRoomCreateResponse.from(restoredRoom));
        }

        // 완전히 새로운 채팅방 생성
        ChatRoom chatRoom = ChatRoom.of(influencerId, advertiserId);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        log.info("새 채팅방 생성 : {}",  savedChatRoom.getId());

        return ServiceResult.ok(ChatRoomCreateResponse.from(savedChatRoom));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResult<ChatRoomListResponse> getChatRoomList(Long currentUserId, String userRole) {
        log.info("ChatRoomService : getChatRoomList({}, {}) 호출", currentUserId, userRole);

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

                boolean hasUnreadMessages = checkUnreadMessages(chatRoom, currentUserId, currentUserRole);
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
    public ServiceResult<ChatMessageListResponse> getChatMessages(Long chatRoomId, String before, String after, int size) {
        log.info("ChatRoomService : getChatMessages({}) 호출", chatRoomId);

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
    public ServiceResult<Void> sendTextMessage(Long chatRoomId, ChatMessageSendRequest request) {
        log.info("ChatRoomService : sendTextMessage({}) 호출", chatRoomId);

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

        chatMessageRepository.save(message);

        // 마지막 읽은 시간 업데이트
        updateLastSeen(chatRoom, request.getUserRole());

        return ServiceResult.ok();
    }

    @Override
    @Transactional
    public ServiceResult<Void> sendFileMessage(Long chatRoomId, Long senderId, String userRole, String messageType, MultipartFile file) {
        log.info("ChatRoomService : sendFileMessage({}) 호출", chatRoomId);

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
        if (chatRoom == null) {
            return ServiceResult.fail(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        UserRole currentUserRole = UserRole.valueOf(userRole.toUpperCase());
        MessageType messageTypeValue = MessageType.valueOf(messageType.toUpperCase());

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        Long fileSize = file.getSize();

        String presignedUploadUrl = s3PresignedUrlService.generatePresignedUploadUrl(fileName, contentType);

        ChatMessage message = ChatMessage.ofFile(
                chatRoomId,
                senderId,
                currentUserRole,
                presignedUploadUrl, // content에 URL 저장
                messageTypeValue,
                fileName,
                fileSize,
                presignedUploadUrl
        );

        chatMessageRepository.save(message);

        // 마지막 읽은 시간 업데이트
        updateLastSeen(chatRoom, currentUserRole);

        return ServiceResult.ok();
    }

    @Override
    @Transactional
    public ServiceResult<Void> leaveChatRoom(Long chatRoomId, Long currentUserId, String userRole) {
        log.info("ChatRoomService : leaveChatRoom({}, {}, {}) 호출", chatRoomId, currentUserId, userRole);

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

    private boolean checkUnreadMessages(ChatRoom chatRoom, Long userId, UserRole userRole) {
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