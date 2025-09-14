package com.ssafy.leaper.domain.influencer.entity;

import com.ssafy.leaper.domain.file.entity.File;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "influencer")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Influencer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "influencer_id")
    private Long influencerId;

    @Column(name = "provider_type_id", length = 31, nullable = false)
    private String providerTypeId;

    @Column(name = "provider_member_id", length = 31, nullable = false)
    private String providerMemberId;

    @Column(name = "nickname", length = 61, nullable = false, unique = true)
    private String nickname;

    @Column(name = "gender", nullable = false)
    private Integer gender; // 남 = 0, 여 = 1

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "influencer_profile_image_id")
    private File profileImage;

    @Column(name = "bio", length = 401)
    private String bio;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    public static Influencer from(String providerTypeId, String providerMemberId, String email) {
        return Influencer.builder()
                .providerTypeId(providerTypeId)
                .providerMemberId(providerMemberId)
                .email(email)
                .isDeleted(false)
                .build();
    }

    public static Influencer of(String providerTypeId, String providerMemberId, String email,
                               String nickname, Integer gender, LocalDate birthday, String bio,
                               File profileImage) {
        return Influencer.builder()
                .providerTypeId(providerTypeId)
                .providerMemberId(providerMemberId)
                .email(email)
                .nickname(nickname)
                .gender(gender)
                .birthday(birthday)
                .bio(bio)
                .profileImage(profileImage)
                .isDeleted(false)
                .build();
    }

    public void updateProfile(String nickname, Integer gender, LocalDate birthday, String bio) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthday = birthday;
        this.bio = bio;
    }
}