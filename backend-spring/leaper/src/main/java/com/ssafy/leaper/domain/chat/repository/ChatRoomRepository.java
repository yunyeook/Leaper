package com.ssafy.leaper.domain.chat.repository;

import com.ssafy.leaper.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

//    /**
//     * 인플루언서와 광고주가 모두 삭제하지 않은 채팅방 조회
//     */
//    @Query("SELECT c FROM ChatRoom c WHERE c.influencerId = :influencerId AND c.advertiserId = :advertiserId " +
//           "AND (c.influencerDeleted = false AND c.advertiserDeleted = false)")
//    Optional<ChatRoom> findByInfluencerIdAndAdvertiserIdAndNotDeleted(
//            @Param("influencerId") Integer influencerId,
//            @Param("advertiserId") Integer advertiserId);

    /**
     * 인플루언서가 삭제하지 않은 채팅방 목록 조회
     */
    @Query("SELECT c FROM ChatRoom c WHERE c.influencerId = :influencerId AND c.influencerDeleted = false " +
           "ORDER BY c.createdAt DESC")
    List<ChatRoom> findByInfluencerIdAndNotDeleted(@Param("influencerId") Integer influencerId);

    /**
     * 광고주가 삭제하지 않은 채팅방 목록 조회
     */
    @Query("SELECT c FROM ChatRoom c WHERE c.advertiserId = :advertiserId AND c.advertiserDeleted = false " +
           "ORDER BY c.createdAt DESC")
    List<ChatRoom> findByAdvertiserIdAndNotDeleted(@Param("advertiserId") Integer advertiserId);

//    /**
//     * 특정 사용자가 참여하고 있는(삭제하지 않은) 채팅방 목록 조회
//     */
//    @Query("SELECT c FROM ChatRoom c WHERE " +
//           "(c.influencerId = :userId AND c.influencerDeleted = false) OR " +
//           "(c.advertiserId = :userId AND c.advertiserDeleted = false) " +
//           "ORDER BY c.createdAt DESC")
//    List<ChatRoom> findByUserIdAndNotDeleted(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE ChatRoom c SET c.influencerLastSeen = :lastSeen WHERE c.id = :chatRoomId")
    void updateInfluencerLastSeen(@Param("chatRoomId") Integer chatRoomId, @Param("lastSeen") LocalDateTime lastSeen);

    @Modifying
    @Query("UPDATE ChatRoom c SET c.advertiserLastSeen = :lastSeen WHERE c.id = :chatRoomId")
    void updateAdvertiserLastSeen(@Param("chatRoomId") Integer chatRoomId, @Param("lastSeen") LocalDateTime lastSeen);

    @Modifying
    @Query("UPDATE ChatRoom c SET c.influencerDeleted = true, c.influencerDeletedAt = :deletedAt WHERE c.id = :chatRoomId")
    void deleteByInfluencer(@Param("chatRoomId") Integer chatRoomId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying
    @Query("UPDATE ChatRoom c SET c.advertiserDeleted = true, c.advertiserDeletedAt = :deletedAt WHERE c.id = :chatRoomId")
    void deleteByAdvertiser(@Param("chatRoomId") Integer chatRoomId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 인플루언서와 광고주 간 채팅방 조회 (삭제된 것 포함)
     */
    @Query("SELECT c FROM ChatRoom c WHERE c.influencerId = :influencerId AND c.advertiserId = :advertiserId")
    Optional<ChatRoom> findByInfluencerIdAndAdvertiserId(
            @Param("influencerId") Integer influencerId,
            @Param("advertiserId") Integer advertiserId);

    /**
     * 인플루언서 삭제 상태 복구
     */
    @Modifying
    @Query("UPDATE ChatRoom c SET c.influencerDeleted = false, c.influencerDeletedAt = null WHERE c.id = :chatRoomId")
    void restoreInfluencer(@Param("chatRoomId") Integer chatRoomId);

    /**
     * 광고주 삭제 상태 복구
     */
    @Modifying
    @Query("UPDATE ChatRoom c SET c.advertiserDeleted = false, c.advertiserDeletedAt = null WHERE c.id = :chatRoomId")
    void restoreAdvertiser(@Param("chatRoomId") Integer chatRoomId);
}