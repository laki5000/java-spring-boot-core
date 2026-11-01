package com.example.core.exception;

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

  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @CsvSource({"'', Internal Server Error", "hu, Váratlan hiba történt"})
  void filterException_shouldReturnLocalizedProblemDetailWithRequestId(
      String language, String expectedTitle) throws Exception {
    // Given
    MockHttpServletRequestBuilder request = get("/test/throwing-filter");

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
        .andExpect(jsonPath("$.title").value(expectedTitle))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").doesNotExist())
        .andExpect(jsonPath("$.requestId").value(requestId));
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
