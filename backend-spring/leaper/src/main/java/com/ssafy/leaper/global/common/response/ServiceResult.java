package com.ssafy.leaper.global.common.response;


import com.ssafy.leaper.global.error.ErrorCode;

public record ServiceResult<T>(
    boolean success,
    T data,
    ErrorCode code
) {
  public static <T> ServiceResult<T> ok(T data) {
    return new ServiceResult<>(true, data, null);
  }

  public static ServiceResult<Void> ok() {
    return new ServiceResult<>(true, null, null);
  }

  public static <T> ServiceResult<T> fail(ErrorCode errorCode) {
    return new ServiceResult<>(false, null, errorCode);
  }
}
