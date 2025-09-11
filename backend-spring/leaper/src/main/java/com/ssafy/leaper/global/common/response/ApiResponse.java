package com.ssafy.leaper.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

  private ResponseStatus status;
  private T data;

  public ApiResponse(ResponseStatus status, T data) {
    this.status = status;
    this.data = data;
  }
}