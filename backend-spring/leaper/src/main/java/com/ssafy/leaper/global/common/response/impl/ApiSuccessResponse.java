package com.ssafy.leaper.global.common.response.impl;


import static com.ssafy.leaper.global.common.response.ResponseStatus.SUCCESS;

import com.ssafy.leaper.global.common.response.ApiResponse;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiSuccessResponse<T> extends ApiResponse<T> {

  public ApiSuccessResponse(T data) {
    super(SUCCESS, data);
  }

  public static <T> ApiResponse<T> success() {
    return success(null);
  }

  public static <T> ApiResponse<Map<String, T>> success(String key, T data) {
    return success(Map.of(key, data));
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiSuccessResponse<>(data);
  }

  public static <T> ResponseEntity<ApiResponse<T>> successEntity(T data) {
    return ResponseEntity.ok(success(data));
  }

  public static <T> ResponseEntity<ApiResponse<Map<String, T>>> successEntity(String key, T data) {
    return ResponseEntity.ok(success(key, data));
  }

  public static ResponseEntity<ApiResponse<Void>> successEntity() {
    return ResponseEntity.ok(success());
  }

}
