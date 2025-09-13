package com.ssafy.leaper.domain.content.dto;

import java.util.List;

public record ContentListResponse(
    List<ContentResponse> contents
) {

    public static ContentListResponse from(List<ContentResponse> contents) {
        return new ContentListResponse(contents);
    }
}