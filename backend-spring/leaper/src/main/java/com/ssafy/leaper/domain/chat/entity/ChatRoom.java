package com.ssafy.leaper.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Integer id;

    @Column(nullable = false)
    private Integer influencerId;

    @Column(nullable = false)
    private Integer advertiserId;

    private LocalDateTime influencerLastSeen;

    private LocalDateTime advertiserLastSeen;

    @Builder.Default
    private Boolean influencerDeleted = false;

    private LocalDateTime influencerDeletedAt;

    @Builder.Default
    private Boolean advertiserDeleted = false;

    private LocalDateTime advertiserDeletedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    public static ChatRoom of(Integer influencerId, Integer advertiserId) {
        return ChatRoom.builder()
                .influencerId(influencerId)
                .advertiserId(advertiserId)
                .build();
    }
}