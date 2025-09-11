package com.ssafy.leaper.global.common.controller;


import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.ServiceResult;
import com.ssafy.leaper.global.common.response.impl.ApiErrorResponse;
import com.ssafy.leaper.global.common.response.impl.ApiSuccessResponse;
import org.springframework.http.ResponseEntity;

public interface BaseController {
  default <T> ResponseEntity<ApiResponse<T>> handle(ServiceResult<T> result) {
    if (!result.success()) {
      return ApiErrorResponse.errorEntity(result.code(), null);
    }
    return ApiSuccessResponse.successEntity(result.data());
  }
}
