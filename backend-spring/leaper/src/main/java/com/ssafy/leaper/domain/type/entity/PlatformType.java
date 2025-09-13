package com.ssafy.leaper.domain.type.entity;

import com.ssafy.leaper.domain.influencer.entity.Influencer;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlatformType {

    @Id
    @Column(name = "platform_type_id", length = 31)
    private String id;

    @Column(nullable = false, length = 31)
    private String typeName;


}
