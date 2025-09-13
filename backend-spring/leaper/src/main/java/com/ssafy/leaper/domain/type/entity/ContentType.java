package com.ssafy.leaper.domain.type.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentType {

    @Id
    @Column(name = "content_type_id", length = 31)
    private String id;

    @Column( nullable = false, length = 31)
    private String typeName;

}
