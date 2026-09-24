package com.example.proj.controller.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerIntegrationTests {

  private static final String DEMO_ENDPOINT = "/api/v1/demo";
  private static final String ERROR_PARAMETER = "error";
  private static final String TRUE_VALUE = "true";
  private static final String FALSE_VALUE = "false";
  private static final String RESPONSE_DATA_PATH = "$.data";
  private static final String RESPONSE_MESSAGE_PATH = "$.message";
  private static final String EXPECTED_RUNNING_MESSAGE = "Core application is running";
  private static final String EXPECTED_EXCEPTION_MESSAGE = "Unexpected error occurred";

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
  void testGetDemo_shouldReturnErrorResponse_whenErrorIsTrue() throws Exception {
    // Given

    // When / Then
    mockMvc
        .perform(get(DEMO_ENDPOINT).param(ERROR_PARAMETER, TRUE_VALUE))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath(RESPONSE_MESSAGE_PATH).value(EXPECTED_EXCEPTION_MESSAGE));
  }
}
