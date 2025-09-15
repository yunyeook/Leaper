package com.ssafy.leaper.domain.chat.repository;

import com.ssafy.leaper.domain.chat.entity.ChatMessage;
import com.ssafy.leaper.global.common.entity.UserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 특정 채팅방의 최신 메시지 조회
     */
    Optional<ChatMessage> findTopByRoomIdOrderByCreatedAtDesc(Integer roomId);

    /**
     * 채팅방의 메시지 목록 조회 (최신순)
     */
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Integer roomId, Pageable pageable);

    /**
     * 특정 ID 이전의 메시지 조회 (이전 페이지 로딩)
     */
    @Query(value = "{'roomId': ?0, '_id': {$lt: ObjectId(?1)}}", sort = "{'createdAt': -1}")
    List<ChatMessage> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(Integer roomId, String beforeId, Pageable pageable);

    /**
     * 특정 ID 이후의 메시지 조회 (재연결 시 놓친 메시지)
     */
    @Query(value = "{'roomId': ?0, '_id': {$gt: ObjectId(?1)}}", sort = "{'createdAt': 1}")
    List<ChatMessage> findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(Integer roomId, String afterId, Pageable pageable);

    /**
     * 특정 시간 이후 메시지 존재 여부 확인 (읽지 않은 메시지 여부)
     */
    boolean existsByRoomIdAndCreatedAtAfter(Integer roomId, Instant createdAt);

    /**
     * 특정 채팅방의 총 메시지 개수
     */
    int countByRoomId(Integer roomId);

    /**
     * 특정 시간 이후의 메시지 개수 (읽지 않은 메시지 개수)
     */
    int countByRoomIdAndCreatedAtAfter(Integer roomId, Instant createdAt);

    /**
     * 특정 시간 이후 상대방이 보낸 메시지 존재 여부 확인 (읽지 않은 메시지 여부)
     */
    boolean existsByRoomIdAndCreatedAtAfterAndSenderIdNot(Integer roomId, Instant createdAt, Integer senderId);

    /**
     * 특정 시간 이후 특정 사용자 타입이 아닌 메시지 존재 여부 확인 (읽지 않은 메시지 여부)
     */
    boolean existsByRoomIdAndCreatedAtAfterAndUserRoleNot(Integer roomId, Instant createdAt, UserRole userRole);
}