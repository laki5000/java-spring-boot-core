package com.example.core.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterUnitTests {

  private static final int MAX_REQUEST_ID_LENGTH = 64;

  private RequestIdFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter = new RequestIdFilter();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  void doFilterInternal_shouldPutGeneratedIdIntoMdcAndResponseHeader() throws Exception {
    // Given
    AtomicReference<String> requestIdInChain = new AtomicReference<>();
    FilterChain filterChain =
        (req, res) -> requestIdInChain.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY));

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

    assertThat(requestId).isEqualTo(UUID.fromString(requestId).toString());
    assertThat(requestIdInChain.get()).isEqualTo(requestId);
    assertThat(MDC.get(RequestIdFilter.REQUEST_ID_KEY)).isNull();
  }

  @ParameterizedTest
  @MethodSource("validRequestIds")
  void doFilterInternal_shouldUseIncomingRequestId_whenItIsValid(String incomingRequestId)
      throws Exception {
    // Given
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, incomingRequestId);

    AtomicReference<String> requestIdInChain = new AtomicReference<>();
    FilterChain filterChain =
        (req, res) -> requestIdInChain.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY));

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(incomingRequestId);
    assertThat(requestIdInChain.get()).isEqualTo(incomingRequestId);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestIds")
  void doFilterInternal_shouldGenerateRequestId_whenIncomingRequestIdIsInvalid(
      String incomingRequestId) throws Exception {
    // Given
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, incomingRequestId);

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

    assertThat(requestId).isEqualTo(UUID.fromString(requestId).toString());
  }

  static Stream<String> validRequestIds() {
    return Stream.of("client-id_1", "a", "x".repeat(MAX_REQUEST_ID_LENGTH));
  }

  static Stream<String> invalidRequestIds() {
    return Stream.of("", "x".repeat(MAX_REQUEST_ID_LENGTH + 1), "id\r\nFAKE", "id with space");
  }

  @Test
  void doFilterInternal_shouldRemoveIdFromMdc_whenFilterChainThrowsException() {
    // Given
    FilterChain filterChain =
        (req, res) -> {
          throw new RuntimeException("Test exception");
        };

    // When
    assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
        .isInstanceOf(RuntimeException.class);

    // Then
    assertThat(MDC.get(RequestIdFilter.REQUEST_ID_KEY)).isNull();
  }
}
