package com.ssafy.spark.domain.business.type.dto.response;

import com.ssafy.spark.domain.business.type.entity.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTypeResponse {

    private String id;
    private String typeName;

    public static PlatformTypeResponse from(PlatformType platformType) {
        return PlatformTypeResponse.builder()
                .id(platformType.getId())
                .typeName(platformType.getTypeName())
                .build();
    }
}