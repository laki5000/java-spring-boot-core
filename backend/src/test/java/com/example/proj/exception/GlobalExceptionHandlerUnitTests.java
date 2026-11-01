package com.example.proj.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerUnitTests {

  private static final String INTERNAL_DETAIL = "Internal detail that must not be exposed";
  private static final Locale HUNGARIAN = Locale.forLanguageTag("hu");
  private static final String BAD_REQUEST_TITLE_KEY = "error.title.400";
  private static final String NOT_FOUND_TITLE_KEY = "error.title.404";
  private static final String HUNGARIAN_BAD_REQUEST_TITLE = "Hibás kérés";
  private static final String HUNGARIAN_NOT_FOUND_TITLE = "Az erőforrás nem található";
  private static final String PARAMETER_NAME = "error";
  private static final String PARAMETER_TYPE = "Boolean";
  private static final String CONSTRAINT_VIOLATION_MESSAGE = "must not be null";
  private static final URI CUSTOM_PROBLEM_TYPE = URI.create("https://example.com/problems/custom");
  private static final String CUSTOM_PROBLEM_TITLE = "Custom problem";
  private static final String MULTILINE_REASON = "Bob\r\nINFO fake log line";
  private static final String ESCAPED_MULTILINE_REASON = "Bob\\r\\nINFO fake log line";
  private static final int LONG_REASON_LENGTH = 600;

  private GlobalExceptionHandler handler;

  private Logger logger;
  private Level originalLevel;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage(BAD_REQUEST_TITLE_KEY, HUNGARIAN, HUNGARIAN_BAD_REQUEST_TITLE);
    messageSource.addMessage(NOT_FOUND_TITLE_KEY, HUNGARIAN, HUNGARIAN_NOT_FOUND_TITLE);

    handler = new GlobalExceptionHandler();
    handler.setMessageSource(messageSource);

    logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    originalLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);

    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
    logger.detachAppender(logAppender);
    logger.setLevel(originalLevel);
  }

  @Test
  void testHandleUnexpectedException_shouldReturnInternalServerError() {
    // Given
    RuntimeException exception = new RuntimeException(INTERNAL_DETAIL);

    // When
    ResponseEntity<Object> response =
        handler.handleUnexpectedException(
            exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
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

  @Test
  void testHandleException_shouldLocalizeTitleAndKeepDetail_whenSpringErrorIsClientError()
      throws Exception {
    // Given
    LocaleContextHolder.setLocale(HUNGARIAN);
    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException(PARAMETER_NAME, PARAMETER_TYPE);

    // When
    ResponseEntity<Object> response =
        handler.handleException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    assertEquals(HUNGARIAN_BAD_REQUEST_TITLE, problemDetail.getTitle());
    assertEquals(exception.getBody().getDetail(), problemDetail.getDetail());
  }

  @Test
  void testHandleConstraintViolation_shouldReturnBadRequest() {
    // Given
    ConstraintViolationException exception =
        new ConstraintViolationException(CONSTRAINT_VIOLATION_MESSAGE, Set.of());

    // When
    ResponseEntity<Object> response =
        handler.handleConstraintViolation(
            exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
  }

  @Test
  void testHandleException_shouldKeepTitle_whenProblemTypeIsCustom() throws Exception {
    // Given
    LocaleContextHolder.setLocale(HUNGARIAN);
    ProblemDetail customProblemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    customProblemDetail.setType(CUSTOM_PROBLEM_TYPE);
    customProblemDetail.setTitle(CUSTOM_PROBLEM_TITLE);
    ErrorResponseException exception =
        new ErrorResponseException(HttpStatus.NOT_FOUND, customProblemDetail, null);

    // When
    ResponseEntity<Object> response =
        handler.handleException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertEquals(CUSTOM_PROBLEM_TITLE, problemDetail.getTitle());
  }

  @Test
  void testHandleException_shouldLogEscapedUntruncatedMessage_whenSpringErrorIsClientError()
      throws Exception {
    // Given
    String reason = "x".repeat(LONG_REASON_LENGTH) + MULTILINE_REASON;
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);

    // When
    handler.handleException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    String message = logAppender.list.getFirst().getFormattedMessage();

    assertTrue(message.contains(reason.replace(MULTILINE_REASON, ESCAPED_MULTILINE_REASON)));
  }

  @Test
  void testHandleException_shouldNotAddRequestId_whenMdcHasNoRequestId() throws Exception {
    // Given
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);

    // When
    ResponseEntity<Object> response =
        handler.handleException(exception, new ServletWebRequest(new MockHttpServletRequest()));

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertNull(problemDetail.getProperties());
  }
}
