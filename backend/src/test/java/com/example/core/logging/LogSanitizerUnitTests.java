package com.example.core.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizerUnitTests {

  private static final String PLAIN_VALUE = "test";
  private static final String NULL_TEXT = "null";
  private static final int MAX_VALUE_LENGTH = 500;
  private static final String TRUNCATED_MARKER = "...(+100 chars)";

  @Test
  void sanitize_shouldReturnText_whenValueIsShortAndSingleLine() {
    // When
    String result = LogSanitizer.sanitize(PLAIN_VALUE);

    // Then
    assertThat(result).isEqualTo(PLAIN_VALUE);
  }

  @Test
  void sanitize_shouldReturnNullText_whenValueIsNull() {
    // When
    String result = LogSanitizer.sanitize(null);

    // Then
    assertThat(result).isEqualTo(NULL_TEXT);
  }

  @Test
  void sanitize_shouldReturnNullText_whenToStringReturnsNull() {
    // When
    String result = LogSanitizer.sanitize(new NullToStringValue());

    // Then
    assertThat(result).isEqualTo(NULL_TEXT);
  }

  @Test
  void sanitize_shouldReturnPlaceholder_whenToStringFails() {
    // When
    String result = LogSanitizer.sanitize(new FailingToStringValue());

    // Then
    assertThat(result)
        .isEqualTo(
            "[LogSanitizerUnitTests.FailingToStringValue, toString failed: IllegalStateException]");
  }

  @Test
  void sanitize_shouldNotTruncate_whenValueHasMaxLength() {
    // Given
    String value = "x".repeat(MAX_VALUE_LENGTH);

    // When
    String result = LogSanitizer.sanitize(value);

    // Then
    assertThat(result).isEqualTo(value);
  }

  @Test
  void sanitize_shouldTruncate_whenValueIsLongerThanMaxLength() {
    // When
    String result = LogSanitizer.sanitize("x".repeat(600));

    // Then
    assertThat(result).isEqualTo("x".repeat(MAX_VALUE_LENGTH) + TRUNCATED_MARKER);
  }

  @Test
  void sanitize_shouldTruncateAfterEscapingLineBreaks() {
    // Given
    int lineFeedCount = MAX_VALUE_LENGTH / 2 + 50;

    // When
    String result = LogSanitizer.sanitize("\n".repeat(lineFeedCount));

    // Then
    assertThat(result).isEqualTo("\\n".repeat(MAX_VALUE_LENGTH / 2) + TRUNCATED_MARKER);
  }

  @Test
  void escapeLineBreaks_shouldEscapeCarriageReturnAndLineFeed() {
    // When
    String result = LogSanitizer.escapeLineBreaks("Bob\r\nINFO fake log line");

    // Then
    assertThat(result).isEqualTo("Bob\\r\\nINFO fake log line");
  }

  @Test
  void escapeLineBreaks_shouldReturnNull_whenTextIsNull() {
    // When
    String result = LogSanitizer.escapeLineBreaks(null);

    // Then
    assertThat(result).isNull();
  }

  static class FailingToStringValue {
    @Override
    public String toString() {
      throw new IllegalStateException();
    }
  }

  static class NullToStringValue {
    @Override
    public String toString() {
      return null;
    }
  }
}
