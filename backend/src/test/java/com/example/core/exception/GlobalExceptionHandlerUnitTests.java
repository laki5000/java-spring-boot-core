package com.example.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.http.HttpStatusCode;
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
  private static final String HUNGARIAN_BAD_REQUEST_TITLE = "Hibás kérés";
  private static final String CUSTOM_PROBLEM_TITLE = "Custom problem";
  private static final String MULTILINE_REASON = "Bob\r\nINFO fake log line";
  private static final String BUSINESS_RULE_MESSAGE = "Business rule violated";

  private GlobalExceptionHandler handler;
  private ServletWebRequest webRequest;

  private Logger logger;
  private Level originalLevel;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("error.title.400", HUNGARIAN, HUNGARIAN_BAD_REQUEST_TITLE);
    messageSource.addMessage("error.title.404", HUNGARIAN, "Az erőforrás nem található");

    handler = new GlobalExceptionHandler();
    handler.setMessageSource(messageSource);

    webRequest = new ServletWebRequest(new MockHttpServletRequest());

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
  void handleUnexpectedException_shouldReturnInternalServerError() {
    // Given
    RuntimeException exception = new RuntimeException(INTERNAL_DETAIL);

    // When
    ResponseEntity<Object> response = handler.handleUnexpectedException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Test
  void handleException_shouldNotExposeDetail_whenSpringErrorIsServerError() throws Exception {
    // Given
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_DETAIL);

    // When
    ResponseEntity<Object> response = handler.handleException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problemDetail.getDetail()).isNull();
  }

  @Test
  void handleException_shouldLocalizeTitleAndKeepDetail_whenSpringErrorIsClientError()
      throws Exception {
    // Given
    LocaleContextHolder.setLocale(HUNGARIAN);
    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("error", "Boolean");

    // When
    ResponseEntity<Object> response = handler.handleException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problemDetail.getTitle()).isEqualTo(HUNGARIAN_BAD_REQUEST_TITLE);
    assertThat(problemDetail.getDetail()).isEqualTo(exception.getBody().getDetail());
  }

  @Test
  void handleConstraintViolation_shouldReturnBadRequest() {
    // Given
    ConstraintViolationException exception =
        new ConstraintViolationException("must not be null", Set.of());

    // When
    ResponseEntity<Object> response = handler.handleConstraintViolation(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void handleException_shouldKeepTitle_whenProblemTypeIsCustom() throws Exception {
    // Given
    LocaleContextHolder.setLocale(HUNGARIAN);
    ProblemDetail customProblemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    customProblemDetail.setType(URI.create("https://example.com/problems/custom"));
    customProblemDetail.setTitle(CUSTOM_PROBLEM_TITLE);
    ErrorResponseException exception =
        new ErrorResponseException(HttpStatus.NOT_FOUND, customProblemDetail, null);

    // When
    ResponseEntity<Object> response = handler.handleException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getTitle()).isEqualTo(CUSTOM_PROBLEM_TITLE);
  }

  @Test
  void handleException_shouldLogEscapedUntruncatedMessage_whenSpringErrorIsClientError()
      throws Exception {
    // Given
    String reason = "x".repeat(600) + MULTILINE_REASON;
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);

    // When
    handler.handleException(exception, webRequest);

    // Then
    String message = logAppender.list.getFirst().getFormattedMessage();

    assertThat(message).contains(reason.replace(MULTILINE_REASON, "Bob\\r\\nINFO fake log line"));
  }

  @Test
  void handleException_shouldNotAddRequestId_whenMdcHasNoRequestId() throws Exception {
    // Given
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);

    // When
    ResponseEntity<Object> response = handler.handleException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getProperties()).isNull();
  }

  @Test
  void handleException_shouldKeepTitleNull_whenStatusIsUnknown() throws Exception {
    // Given
    ResponseStatusException exception = new ResponseStatusException(HttpStatusCode.valueOf(599));

    // When
    ResponseEntity<Object> response = handler.handleException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(599);
    assertThat(problemDetail.getTitle()).isNull();
  }

  @Test
  void handleBusinessException_shouldReturnUnprocessableContentWithMessageAsDetail() {
    // Given
    BusinessException exception = new BusinessException(BUSINESS_RULE_MESSAGE) {};

    // When
    ResponseEntity<Object> response = handler.handleBusinessException(exception, webRequest);

    // Then
    ProblemDetail problemDetail = (ProblemDetail) response.getBody();

    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
    assertThat(problemDetail.getDetail()).isEqualTo(BUSINESS_RULE_MESSAGE);
  }
}
