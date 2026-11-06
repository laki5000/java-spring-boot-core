package com.example.core.exception;

import com.example.core.logging.LogSanitizer;
import com.example.core.logging.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final URI DEFAULT_PROBLEM_TYPE = URI.create("about:blank");
  private static final String TITLE_MESSAGE_PREFIX = "error.title.";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Object> handleBusinessException(
      BusinessException exception, WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());

    return handleExceptionInternal(
        exception, problemDetail, HttpHeaders.EMPTY, HttpStatus.UNPROCESSABLE_CONTENT, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(
      ConstraintViolationException exception, WebRequest request) {

    return handleExceptionInternal(
        exception, null, HttpHeaders.EMPTY, HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {

    return handleExceptionInternal(
        exception, null, HttpHeaders.EMPTY, HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    if (status.is5xxServerError()) {
      log.error("Request failed with status {}", status.value(), exception);
    } else {
      log.debug(
          "Request failed with status {}: {}",
          status.value(),
          LogSanitizer.escapeLineBreaks(exception.getMessage()));
    }

    ProblemDetail problemDetail =
        body instanceof ProblemDetail existing ? existing : createProblemDetail(exception, status);

    if (status.is5xxServerError()) {
      problemDetail.setDetail(null);
    }

    String requestId = MDC.get(RequestIdFilter.REQUEST_ID_KEY);

    if (requestId != null) {
      problemDetail.setProperty(RequestIdFilter.REQUEST_ID_KEY, requestId);
    }

    if (hasDefaultType(problemDetail)) {
      problemDetail.setTitle(getLocalizedTitle(status, problemDetail.getTitle()));
    }

    return super.handleExceptionInternal(exception, problemDetail, headers, status, request);
  }

  private ProblemDetail createProblemDetail(Exception exception, HttpStatusCode status) {
    if (exception instanceof ErrorResponse errorResponse) {
      return errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    }

    return ProblemDetail.forStatus(status);
  }

  private boolean hasDefaultType(ProblemDetail problemDetail) {
    URI type = problemDetail.getType();

    return type == null || DEFAULT_PROBLEM_TYPE.equals(type);
  }

  private String getLocalizedTitle(HttpStatusCode status, String defaultTitle) {
    return getMessageSource()
        .getMessage(
            TITLE_MESSAGE_PREFIX + status.value(),
            null,
            defaultTitle,
            LocaleContextHolder.getLocale());
  }
}
