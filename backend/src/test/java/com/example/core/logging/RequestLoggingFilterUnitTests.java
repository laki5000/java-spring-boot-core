package com.example.core.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterUnitTests {

  private static final String REQUEST_URI = "/api/demo";
  private static final String HTTP_METHOD = "GET";
  private static final String EXCEPTION_MESSAGE = "Test exception";
  private static final String EXPECTED_LOG_PATTERN = "GET /api/demo -> 200 \\(\\d+ ms\\)";

  private RequestLoggingFilter filter;
  private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    filter = new RequestLoggingFilter();

    Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);

    logAppender = new ListAppender<>();
    logAppender.start();

    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);

    logger.detachAppender(logAppender);
  }

  @Test
  void doFilterInternal_logsRequest_whenRequestCompletes() throws Exception {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest(HTTP_METHOD, REQUEST_URI);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = (req, res) -> {};

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(logAppender.list).hasSize(1);

    var logEvent = logAppender.list.getFirst();

    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
    assertThat(logEvent.getFormattedMessage()).matches(EXPECTED_LOG_PATTERN);
  }

  @Test
  void doFilterInternal_logsRequest_whenFilterChainThrowsException() {
    // Given
    MockHttpServletRequest request = new MockHttpServletRequest(HTTP_METHOD, REQUEST_URI);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        (req, res) -> {
          throw new RuntimeException(EXCEPTION_MESSAGE);
        };

    // When
    assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));

    // Then
    assertThat(logAppender.list).hasSize(1);

    var logEvent = logAppender.list.getFirst();

    assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
    assertThat(logEvent.getFormattedMessage()).matches(EXPECTED_LOG_PATTERN);
  }
}
