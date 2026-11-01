package com.example.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.core.message.I18nService;
import com.example.project.exception.DemoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoServiceUnitTests {

  private static final String DEMO_RUNNING_MESSAGE = "Demo is running";
  private static final String DEMO_RUNNING_MESSAGE_KEY = "demo.running";

  @Mock private I18nService i18nService;

  private DemoService demoService;

  @BeforeEach
  void setUp() {
    demoService = new DemoService(i18nService);
  }

  @Test
  void getDemoMessage_shouldReturnRunningMessage_whenErrorIsFalse() {
    // Given
    when(i18nService.getMessage(DEMO_RUNNING_MESSAGE_KEY)).thenReturn(DEMO_RUNNING_MESSAGE);

    // When
    String result = demoService.getDemoMessage(false);

    // Then
    assertThat(result).isEqualTo(DEMO_RUNNING_MESSAGE);
    verify(i18nService).getMessage(DEMO_RUNNING_MESSAGE_KEY);
  }

  @Test
  void getDemoMessage_shouldThrowException_whenErrorIsTrue() {
    // When / Then
    assertThatThrownBy(() -> demoService.getDemoMessage(true))
        .isInstanceOf(DemoException.class)
        .hasMessage("Demo error was requested");
  }

  @Test
  void getDemoMessage_shouldReturnRunningMessage_whenErrorIsNull() {
    // Given
    when(i18nService.getMessage(DEMO_RUNNING_MESSAGE_KEY)).thenReturn(DEMO_RUNNING_MESSAGE);

    // When
    String result = demoService.getDemoMessage(null);

    // Then
    assertThat(result).isEqualTo(DEMO_RUNNING_MESSAGE);
    verify(i18nService).getMessage(DEMO_RUNNING_MESSAGE_KEY);
  }
}
