package com.ssafy.leaper.domain.type.dto.response;

import com.ssafy.leaper.domain.type.entity.CategoryType;

public record CategoryTypeResponse(
    Short id,
    String categoryName
) {
    public static CategoryTypeResponse from(CategoryType categoryType) {
        return new CategoryTypeResponse(
            categoryType.getId(),
            categoryType.getCategoryName()
        );
    }
}