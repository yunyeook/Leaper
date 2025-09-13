package com.ssafy.leaper.domain.content.dto.response;

import java.util.List;

public record SimilarContentListResponse(
    List<ContentDetailResponse> contents
) {
  public static SimilarContentListResponse from(List<ContentDetailResponse> contents) {
    return new SimilarContentListResponse(contents);
  }
}
