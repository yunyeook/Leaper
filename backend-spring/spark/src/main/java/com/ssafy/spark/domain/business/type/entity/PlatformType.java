package com.ssafy.spark.domain.business.type.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
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
