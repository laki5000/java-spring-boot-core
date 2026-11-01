package com.example.proj.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerUnitTests {

  private static final String INTERNAL_DETAIL = "Internal detail that must not be exposed";

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    handler.setMessageSource(new StaticMessageSource());
  }

  @Test
  void testHandleException_shouldNotExposeDetail_whenSpringErrorIsServerError() throws Exception {
    // Given
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_DETAIL);

    // When
    ResponseEntity<Object> response =
        handler.handleException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
    assertNull(problemDetail.getDetail());
  }
}
