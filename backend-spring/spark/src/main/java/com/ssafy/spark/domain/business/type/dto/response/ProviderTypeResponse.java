package com.ssafy.spark.domain.business.type.dto.response;

import com.ssafy.spark.domain.business.type.entity.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderTypeResponse {

    private String id;
    private String typeName;

    public static ProviderTypeResponse from(ProviderType providerType) {
        return ProviderTypeResponse.builder()
                .id(providerType.getId())
                .typeName(providerType.getTypeName())
                .build();
    }
}