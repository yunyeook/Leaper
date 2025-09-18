package com.ssafy.leaper.domain.type.dto.response;

import com.ssafy.leaper.domain.type.entity.ContentType;

public record ContentTypeResponse(
    String id,
    String typeName
) {
    public static ContentTypeResponse from(ContentType contentType) {
        return new ContentTypeResponse(
            contentType.getId(),
            contentType.getTypeName()
        );
    }
}