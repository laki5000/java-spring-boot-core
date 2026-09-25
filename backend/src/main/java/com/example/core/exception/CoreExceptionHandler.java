package com.example.core.exception;

import com.example.core.message.I18nService;
import com.example.generated.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CoreExceptionHandler {

  private static final String UNEXPECTED_ERROR_MESSAGE = "error.unexpected";
  private static final String BAD_REQUEST_ERROR_MESSAGE = "error.bad-request";
  private static final String NOT_FOUND_ERROR_MESSAGE = "error.not-found";
  private static final String METHOD_NOT_ALLOWED_ERROR_MESSAGE = "error.method-not-allowed";
  private static final String UNSUPPORTED_MEDIA_TYPE_ERROR_MESSAGE = "error.unsupported-media-type";

  private final I18nService i18nService;

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
    log.warn("Bad request: {}", exception.getMessage());

    return createResponse(HttpStatus.BAD_REQUEST, BAD_REQUEST_ERROR_MESSAGE);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException exception) {

    log.warn("Resource not found: {}", exception.getMessage());

    return createResponse(HttpStatus.NOT_FOUND, NOT_FOUND_ERROR_MESSAGE);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException exception) {

    log.warn("Method not allowed: {}", exception.getMessage());

    return createResponse(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_ERROR_MESSAGE);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException exception) {

    log.warn("Unsupported media type: {}", exception.getMessage());

    return createResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_ERROR_MESSAGE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unexpected exception occurred", exception);

    return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE);
  }

  private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String messageKey) {

    ErrorResponse response = new ErrorResponse();
    response.setMessage(i18nService.getMessage(messageKey));

    return ResponseEntity.status(status).body(response);
  }
}
