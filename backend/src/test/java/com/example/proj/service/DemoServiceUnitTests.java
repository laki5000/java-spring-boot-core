package com.example.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.core.message.I18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoServiceUnitTests {

  private static final String DEMO_RUNNING_MESSAGE = "Demo is running";
  private static final String DEMO_EXCEPTION_MESSAGE = "Example exception";
  private static final String DEMO_RUNNING_MESSAGE_KEY = "demo.running";
  private static final String DEMO_EXCEPTION_MESSAGE_KEY = "demo.example.exception";

  @Mock private I18nService i18nService;

  private DemoService demoService;

  @BeforeEach
  void setUp() {
    demoService = new DemoService(i18nService);
  }

  @Test
  void testGetDemoMessage_shouldReturnRunningMessage_whenErrorIsFalse() {
    // Given
    when(i18nService.getMessage(DEMO_RUNNING_MESSAGE_KEY)).thenReturn(DEMO_RUNNING_MESSAGE);

    // When
    String result = demoService.getDemoMessage(false);

    // Then
    assertEquals(DEMO_RUNNING_MESSAGE, result);
    verify(i18nService).getMessage(DEMO_RUNNING_MESSAGE_KEY);
  }

  @Test
  void testGetDemoMessage_shouldThrowException_whenErrorIsTrue() {
    // When / Then
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> demoService.getDemoMessage(true));

    assertEquals(DEMO_EXCEPTION_MESSAGE, exception.getMessage());
  }

  @Test
  void testGetDemoMessage_shouldReturnRunningMessage_whenErrorIsNull() {
    // Given
    when(i18nService.getMessage(DEMO_RUNNING_MESSAGE_KEY)).thenReturn(DEMO_RUNNING_MESSAGE);

    // When
    String result = demoService.getDemoMessage(null);

    // Then
    assertEquals(DEMO_RUNNING_MESSAGE, result);
    verify(i18nService).getMessage(DEMO_RUNNING_MESSAGE_KEY);
  }
}
