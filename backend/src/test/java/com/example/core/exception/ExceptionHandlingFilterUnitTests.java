package com.example.core.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private RuntimeException exception;
  private FilterChain throwingFilterChain;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    exception = new RuntimeException("Test exception");
    throwingFilterChain =
        (req, res) -> {
          throw exception;
        };
  }

  @Test
  void doFilterInternal_shouldPassExceptionToResolver_whenFilterChainThrowsException() {
    // Given
    AtomicReference<Exception> resolvedException = new AtomicReference<>();
    ExceptionHandlingFilter filter = recordingFilter(resolvedException);

    // When
    assertThatCode(() -> filter.doFilter(request, response, throwingFilterChain))
        .doesNotThrowAnyException();

    // Then
    assertThat(resolvedException.get()).isSameAs(exception);
    assertThat(LocaleContextHolder.getLocaleContext()).isNull();
  }

  @Test
  void doFilterInternal_shouldRethrowException_whenResolverCannotHandleIt() {
    // Given
    ExceptionHandlingFilter filter =
        new ExceptionHandlingFilter(
            (req, res, handler, ex) -> null, new AcceptHeaderLocaleResolver());

    // When
    RuntimeException thrown =
        catchThrowableOfType(
            RuntimeException.class, () -> filter.doFilter(request, response, throwingFilterChain));

    // Then
    assertThat(thrown).isSameAs(exception);
  }

  @Test
  void doFilterInternal_shouldRethrowExceptionWithoutResolving_whenResponseIsCommitted() {
    // Given
    AtomicReference<Exception> resolvedException = new AtomicReference<>();
    ExceptionHandlingFilter filter = recordingFilter(resolvedException);
    response.setCommitted(true);

    // When
    RuntimeException thrown =
        catchThrowableOfType(
            RuntimeException.class, () -> filter.doFilter(request, response, throwingFilterChain));

    // Then
    assertThat(thrown).isSameAs(exception);
    assertThat(resolvedException.get()).isNull();
  }

  @Test
  void doFilterInternal_shouldClearPartialResponse_beforeResolvingException() {
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
          res.setContentType("text/plain");
          res.getWriter().write("partial");
          throw exception;
        };

    // When
    assertThatCode(() -> filter.doFilter(request, response, partiallyWritingFilterChain))
        .doesNotThrowAnyException();

    // Then
    assertThat(contentTypeAtResolution.get()).isNull();
    assertThat(bufferedBytesAtResolution.get()).isEqualTo(0);
  }

  private ExceptionHandlingFilter recordingFilter(AtomicReference<Exception> resolvedException) {
    return new ExceptionHandlingFilter(
        (req, res, handler, ex) -> {
          resolvedException.set(ex);
          return new ModelAndView();
        },
        new AcceptHeaderLocaleResolver());
  }
}
