package com.ssafy.leaper.domain.type.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    @Column(name = "type_name", nullable = false, length = 31)
    private String typeName;


}
