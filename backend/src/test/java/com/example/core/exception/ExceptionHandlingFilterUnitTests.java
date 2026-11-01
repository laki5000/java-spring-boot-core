package com.example.core.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class ExceptionHandlingFilterUnitTests {

  private static final String EXCEPTION_MESSAGE = "Test exception";
  private static final String PARTIAL_CONTENT_TYPE = "text/plain";
  private static final String PARTIAL_BODY = "partial";

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private RuntimeException exception;
  private FilterChain throwingFilterChain;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    exception = new RuntimeException(EXCEPTION_MESSAGE);
    throwingFilterChain =
        (req, res) -> {
          throw exception;
        };
  }

  @Test
  void doFilterInternal_passesExceptionToResolver_whenFilterChainThrowsException() {
    // Given
    AtomicReference<Exception> resolvedException = new AtomicReference<>();
    ExceptionHandlingFilter filter =
        new ExceptionHandlingFilter(
            (req, res, handler, ex) -> {
              resolvedException.set(ex);
              return new ModelAndView();
            },
            new AcceptHeaderLocaleResolver());

    // When
    assertDoesNotThrow(() -> filter.doFilter(request, response, throwingFilterChain));

    // Then
    assertSame(exception, resolvedException.get());
    assertNull(LocaleContextHolder.getLocaleContext());
  }

  @Test
  void doFilterInternal_rethrowsException_whenResolverCannotHandleIt() {
    // Given
    ExceptionHandlingFilter filter =
        new ExceptionHandlingFilter(
            (req, res, handler, ex) -> null, new AcceptHeaderLocaleResolver());

    // When
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> filter.doFilter(request, response, throwingFilterChain));

    // Then
    assertSame(exception, thrown);
  }

  @Test
  void doFilterInternal_rethrowsExceptionWithoutResolving_whenResponseIsCommitted() {
    // Given
    AtomicReference<Exception> resolvedException = new AtomicReference<>();
    ExceptionHandlingFilter filter =
        new ExceptionHandlingFilter(
            (req, res, handler, ex) -> {
              resolvedException.set(ex);
              return new ModelAndView();
            },
            new AcceptHeaderLocaleResolver());
    response.setCommitted(true);

    // When
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> filter.doFilter(request, response, throwingFilterChain));

    // Then
    assertSame(exception, thrown);
    assertNull(resolvedException.get());
  }

  @Test
  void doFilterInternal_clearsPartialResponse_beforeResolvingException() {
    // Given
    AtomicReference<String> contentTypeAtResolution = new AtomicReference<>();
    AtomicReference<Integer> bufferedBytesAtResolution = new AtomicReference<>();
    ExceptionHandlingFilter filter =
        new ExceptionHandlingFilter(
            (req, res, handler, ex) -> {
              contentTypeAtResolution.set(res.getContentType());
              bufferedBytesAtResolution.set(response.getContentAsByteArray().length);
              return new ModelAndView();
            },
            new AcceptHeaderLocaleResolver());
    FilterChain partiallyWritingFilterChain =
        (req, res) -> {
          res.setContentType(PARTIAL_CONTENT_TYPE);
          res.getWriter().write(PARTIAL_BODY);
          throw exception;
        };

    // When
    assertDoesNotThrow(() -> filter.doFilter(request, response, partiallyWritingFilterChain));

    // Then
    assertNull(contentTypeAtResolution.get());
    assertEquals(0, bufferedBytesAtResolution.get());
  }
}
