package com.ssafy.leaper.domain.advertiser.entity;

import com.ssafy.leaper.domain.file.entity.File;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Advertiser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advertiser_id")
    private Integer id;
    
    @Column(nullable = false, length = 21)
    private String loginId;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, length = 61)
    private String brandName;
    
    @Column(nullable = false, length = 91)
    private String companyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_profile_image_id")
    private File profileImage;
    
    @Column(nullable = false, length = 100)
    private String representativeName;
    
    @Column(nullable = false, length = 10)
    private String businessRegNo;
    
    private LocalDate openingDate;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isDeleted;
    
    private LocalDateTime deletedAt;
}