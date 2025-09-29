package com.ssafy.spark.domain.business.type.dto.response;

import com.ssafy.spark.domain.business.type.entity.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTypeResponse {

    private Short id;
    private String categoryName;

    public static CategoryTypeResponse from(CategoryType categoryType) {
        return CategoryTypeResponse.builder()
                .id(categoryType.getId())
                .categoryName(categoryType.getCategoryName())
                .build();
    }
}