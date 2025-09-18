package com.ssafy.leaper.domain.type.dto.response;

import com.ssafy.leaper.domain.type.entity.ProviderType;

public record ProviderTypeResponse(
    String id,
    String typeName
) {
    public static ProviderTypeResponse from(ProviderType providerType) {
        return new ProviderTypeResponse(
            providerType.getId(),
            providerType.getTypeName()
        );
    }
}