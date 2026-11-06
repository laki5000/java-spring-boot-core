package com.example.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiExceptionUnitTests {

  @Test
  void getMessage_shouldContainMessageKeyAndArguments() {
    // When
    BusinessException exception = new BusinessException("test.businessRule", "abc", 42);

    // Then
    assertThat(exception.getMessage()).isEqualTo("test.businessRule [abc, 42]");
  }

  @Test
  void getMessage_shouldBeMessageKey_whenThereAreNoArguments() {
    // When
    BusinessException exception = new BusinessException("test.businessRule");

    // Then
    assertThat(exception.getMessage()).isEqualTo("test.businessRule");
  }
}
