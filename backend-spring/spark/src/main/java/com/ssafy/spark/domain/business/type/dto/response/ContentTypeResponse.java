package com.ssafy.spark.domain.business.type.dto.response;

import com.ssafy.spark.domain.business.type.entity.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTypeResponse {

    private String id;
    private String typeName;

    public static ContentTypeResponse from(ContentType contentType) {
        return ContentTypeResponse.builder()
                .id(contentType.getId())
                .typeName(contentType.getTypeName())
                .build();
    }
}