package com.example.core.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ExceptionHandlingFilterIntegrationTests {

  private static final String THROWING_PATH = "/test/throwing-filter";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String RESPONSE_TITLE_PATH = "$.title";
  private static final String RESPONSE_STATUS_PATH = "$.status";
  private static final String RESPONSE_DETAIL_PATH = "$.detail";
  private static final String RESPONSE_REQUEST_ID_PATH = "$.requestId";

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @CsvSource({"'', Internal Server Error", "hu, Váratlan hiba történt"})
  void testFilterException_shouldReturnLocalizedProblemDetailWithRequestId(
      String language, String expectedTitle) throws Exception {
    // Given
    MockHttpServletRequestBuilder request = get(THROWING_PATH);

    if (!language.isEmpty()) {
      request.header(HttpHeaders.ACCEPT_LANGUAGE, language);
    }

    // When
    ResultActions result = mockMvc.perform(request);

    // Then
    String requestId = result.andReturn().getResponse().getHeader(REQUEST_ID_HEADER);

    result
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath(RESPONSE_TITLE_PATH).value(expectedTitle))
        .andExpect(jsonPath(RESPONSE_STATUS_PATH).value(500))
        .andExpect(jsonPath(RESPONSE_DETAIL_PATH).doesNotExist())
        .andExpect(jsonPath(RESPONSE_REQUEST_ID_PATH).value(requestId));
  }

  @TestConfiguration
  static class ThrowingFilterConfig {

    @Bean
    Filter throwingFilter() {
      return (request, response, chain) -> {
        throw new IllegalStateException("Filter failure");
      };
    }
  }
}
