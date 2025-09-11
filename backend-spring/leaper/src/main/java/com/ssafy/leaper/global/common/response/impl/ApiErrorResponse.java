package com.ssafy.leaper.global.common.response.impl;


import static com.ssafy.leaper.global.common.response.ResponseStatus.ERROR;

import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.ResponseStatus;
import com.ssafy.leaper.global.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiErrorResponse<T> extends ApiResponse<T> {
  private String code;

  public ApiErrorResponse(ResponseStatus status, T data, String code) {
    super(status, data);
    this.code = code;
  }

  public static ApiResponse<Void> error(ErrorCode errorCode) {
    return error(errorCode.getCode(), null);
  }

  public static <T> ApiResponse<T> error(String code, T data) {
    return new ApiErrorResponse<>(ERROR, data, code);
  }

  public static <T> ResponseEntity<ApiResponse<T>> errorEntity(ErrorCode code, T data) {
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(error(code.getCode(), data));
  }

  public static ResponseEntity<ApiResponse<Void>> errorEntity(ErrorCode errorCode) {
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(error(errorCode));
  }

}
