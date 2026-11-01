package com.example.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  private static final String EXCEPTION_MESSAGE = "Test exception";

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
  void doFilterInternal_putsGeneratedIdIntoMdcAndResponseHeader() throws Exception {
    // Given
    AtomicReference<String> requestIdInChain = new AtomicReference<>();
    FilterChain filterChain =
        (req, res) -> requestIdInChain.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY));

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

    assertEquals(UUID.fromString(requestId).toString(), requestId);
    assertEquals(requestId, requestIdInChain.get());
    assertNull(MDC.get(RequestIdFilter.REQUEST_ID_KEY));
  }

  @ParameterizedTest
  @MethodSource("validRequestIds")
  void doFilterInternal_usesIncomingRequestId_whenItIsValid(String incomingRequestId)
      throws Exception {
    // Given
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, incomingRequestId);

    AtomicReference<String> requestIdInChain = new AtomicReference<>();
    FilterChain filterChain =
        (req, res) -> requestIdInChain.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY));

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertEquals(incomingRequestId, response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
    assertEquals(incomingRequestId, requestIdInChain.get());
  }

  @ParameterizedTest
  @MethodSource("invalidRequestIds")
  void doFilterInternal_generatesRequestId_whenIncomingRequestIdIsInvalid(String incomingRequestId)
      throws Exception {
    // Given
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, incomingRequestId);

    // When
    filter.doFilter(request, response, (req, res) -> {});

    // Then
    String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

    assertEquals(UUID.fromString(requestId).toString(), requestId);
  }

  static Stream<String> validRequestIds() {
    return Stream.of("client-id_1", "a", "x".repeat(MAX_REQUEST_ID_LENGTH));
  }

  static Stream<String> invalidRequestIds() {
    return Stream.of("", "x".repeat(MAX_REQUEST_ID_LENGTH + 1), "id\r\nFAKE", "id with space");
  }

  @Test
  void doFilterInternal_removesIdFromMdc_whenFilterChainThrowsException() {
    // Given
    FilterChain filterChain =
        (req, res) -> {
          throw new RuntimeException(EXCEPTION_MESSAGE);
        };

    // When
    assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));

    // Then
    assertNull(MDC.get(RequestIdFilter.REQUEST_ID_KEY));
  }
}
