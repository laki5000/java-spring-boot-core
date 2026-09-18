package com.example.core.exception;

import com.example.core.message.I18nService;
import com.example.generated.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CoreExceptionHandler {

  private static final String UNEXPECTED_ERROR_MESSAGE = "error.unexpected";

  private final I18nService i18nService;

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unexpected exception occurred", exception);

    ErrorResponse response = new ErrorResponse();
    response.setMessage(i18nService.getMessage(UNEXPECTED_ERROR_MESSAGE));

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
