package com.ssafy.leaper.global.error.exception;


import com.ssafy.leaper.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getCode());
    this.errorCode = errorCode;
  }

  public BusinessException(String message, ErrorCode errorCode) {
    super(message + " : " + errorCode.getCode());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getCode(), cause);
    this.errorCode = errorCode;
  }

  public BusinessException(String message, ErrorCode errorCode, Throwable cause) {
    super(message + " : " + errorCode.getCode(), cause);
    this.errorCode = errorCode;
  }
}
