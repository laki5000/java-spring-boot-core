package com.example.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.core.message.I18nService;
import com.example.project.exception.DemoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoServiceUnitTests {

  private static final String DEMO_RUNNING_MESSAGE = "Demo is running";

  @Mock private I18nService i18nService;

  private DemoService demoService;

  @BeforeEach
  void setUp() {
    demoService = new DemoService(i18nService);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  void getDemoMessage_shouldReturnRunningMessage_whenErrorIsNotTrue(Boolean error) {
    // Given
    when(i18nService.getMessage("demo.running")).thenReturn(DEMO_RUNNING_MESSAGE);

    // When
    String result = demoService.getDemoMessage(error);

    // Then
    assertThat(result).isEqualTo(DEMO_RUNNING_MESSAGE);
  }

  @Test
  void getDemoMessage_shouldThrowException_whenErrorIsTrue() {
    // When / Then
    assertThatThrownBy(() -> demoService.getDemoMessage(true))
        .isInstanceOf(DemoException.class)
        .hasMessage("Demo error was requested");
  }
}
