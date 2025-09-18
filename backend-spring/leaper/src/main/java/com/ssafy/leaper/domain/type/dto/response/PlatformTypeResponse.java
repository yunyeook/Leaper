package com.ssafy.leaper.domain.type.dto.response;

import com.ssafy.leaper.domain.type.entity.PlatformType;

public record PlatformTypeResponse(
    String id,
    String typeName
) {
    public static PlatformTypeResponse from(PlatformType platformType) {
        return new PlatformTypeResponse(
            platformType.getId(),
            platformType.getTypeName()
        );
    }
}