package com.ssafy.leaper.global.error;


import static com.ssafy.leaper.global.error.ErrorCode.*;
import static java.util.stream.Collectors.toList;

import com.ssafy.leaper.global.common.response.ApiResponse;
import com.ssafy.leaper.global.common.response.impl.ApiErrorResponse;
import com.ssafy.leaper.global.error.exception.BusinessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 전역 예외 처리하는 역할
public class GlobalExceptionHandler {

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ApiResponse<Map<String, Object>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    List<ErrorDetail> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new ErrorDetail(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
            .collect(toList());

    Map<String, Object> data = new HashMap<>();
    data.put("errors", errors);
    return ApiErrorResponse.error(
        COMMON_INVALID_FORMAT.getCode(), data);
  }

  @ExceptionHandler(value = BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiErrorResponse.error(errorCode));
  }

  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    log.error("서버 에러 발생 : ", e);
    return ResponseEntity.status(500).body(e.getMessage());
  }
}
