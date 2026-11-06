package com.example.core.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

  public BusinessException(String messageKey, Object... arguments) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, messageKey, arguments);
  }
}
