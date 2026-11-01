package com.example.proj.controller.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerIntegrationTests {

  private static final String DEMO_ENDPOINT = "/api/v1/demo";
  private static final String ERROR_PARAMETER = "error";
  private static final String TRUE_VALUE = "true";
  private static final String FALSE_VALUE = "false";
  private static final String RESPONSE_DATA_PATH = "$.data";
  private static final String RESPONSE_TITLE_PATH = "$.title";
  private static final String RESPONSE_STATUS_PATH = "$.status";
  private static final String RESPONSE_DETAIL_PATH = "$.detail";
  private static final String RESPONSE_INSTANCE_PATH = "$.instance";
  private static final String EXPECTED_RUNNING_MESSAGE = "Core application is running";
  private static final String EXPECTED_UNPROCESSABLE_CONTENT_TITLE = "Unprocessable Content";
  private static final String EXPECTED_DEMO_ERROR_DETAIL = "Demo error was requested";

  @Autowired private MockMvc mockMvc;

  @Test
  void testGetDemo_shouldReturnRunningMessage_whenErrorIsFalse() throws Exception {
    // Given

    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT).param(ERROR_PARAMETER, FALSE_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath(RESPONSE_DATA_PATH).value(EXPECTED_RUNNING_MESSAGE));
  }

  @Test
  void testGetDemo_shouldReturnRunningMessage_whenErrorIsNull() throws Exception {
    // Given

    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath(RESPONSE_DATA_PATH).value(EXPECTED_RUNNING_MESSAGE));
  }

  @Test
  void testGetDemo_shouldReturnUnprocessableContent_whenErrorIsTrue() throws Exception {
    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT).param(ERROR_PARAMETER, TRUE_VALUE))
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath(RESPONSE_TITLE_PATH).value(EXPECTED_UNPROCESSABLE_CONTENT_TITLE))
        .andExpect(jsonPath(RESPONSE_STATUS_PATH).value(422))
        .andExpect(jsonPath(RESPONSE_DETAIL_PATH).value(EXPECTED_DEMO_ERROR_DETAIL))
        .andExpect(jsonPath(RESPONSE_INSTANCE_PATH).value(DEMO_ENDPOINT));
  }
}
