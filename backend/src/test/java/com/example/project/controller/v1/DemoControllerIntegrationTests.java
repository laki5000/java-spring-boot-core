package com.example.project.controller.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"app.locale.default-locale=en", "app.locale.supported-locales=en"})
@AutoConfigureMockMvc
class DemoControllerIntegrationTests {

  private static final String DEMO_ENDPOINT = "/api/v1/demo";
  private static final String ERROR_PARAMETER = "error";
  private static final String RESPONSE_DATA_PATH = "$.data";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String EXPECTED_RUNNING_MESSAGE = "Core application is running";

  @Autowired private MockMvc mockMvc;

  @Test
  void getDemo_shouldReturnRunningMessage_whenErrorIsFalse() throws Exception {
    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT).param(ERROR_PARAMETER, "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(RESPONSE_DATA_PATH).value(EXPECTED_RUNNING_MESSAGE));
  }

  @Test
  void getDemo_shouldReturnRunningMessage_whenErrorIsNull() throws Exception {
    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath(RESPONSE_DATA_PATH).value(EXPECTED_RUNNING_MESSAGE));
  }

  @Test
  void getDemo_shouldReturnUnprocessableContent_whenErrorIsTrue() throws Exception {
    // When
    ResultActions result = mockMvc.perform(get(DEMO_ENDPOINT).param(ERROR_PARAMETER, "true"));

    // Then
    String requestId = result.andReturn().getResponse().getHeader(REQUEST_ID_HEADER);

    result
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Unprocessable Content"))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.detail").value("Demo error was requested"))
        .andExpect(jsonPath("$.instance").value(DEMO_ENDPOINT))
        .andExpect(header().exists(REQUEST_ID_HEADER))
        .andExpect(jsonPath("$.requestId").value(requestId));
  }
}
