package com.ssafy.leaper.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  /* 1. COMMON – 공통 */
  COMMON_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "COMMON-001"),
  COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002"),
  COMMON_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON-003"),

  /* 2. AUTH – 인증/인가 */
  AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001"),
  AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002"),
  AUTH_DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, "AUTH-003"),

  /* 3. USER – 사용자 */
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001"),
  USER_DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-002"),
  USER_NICKNAME_CHANGE_LIMITED(HttpStatus.FORBIDDEN, "USER-003"),
  USER_RE_REGISTRATION_FORBIDDEN(HttpStatus.FORBIDDEN, "USER-004"),
  USER_LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "USER-005"),
  USER_SIGN_UP_FAIL(HttpStatus.UNAUTHORIZED, "USER-006"),

  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-001");

  private final HttpStatus httpStatus;
  private final String code;
}
