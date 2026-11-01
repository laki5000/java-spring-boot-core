package com.example.core.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LogSanitizerUnitTests {

  private static final String PLAIN_VALUE = "test";
  private static final String NULL_TEXT = "null";
  private static final String MULTILINE_VALUE = "Bob\r\nINFO fake log line";
  private static final String ESCAPED_MULTILINE_VALUE = "Bob\\r\\nINFO fake log line";
  private static final String LINE_FEED = "\n";
  private static final String ESCAPED_LINE_FEED = "\\n";
  private static final int MAX_VALUE_LENGTH = 500;
  private static final int LONG_VALUE_LENGTH = 600;
  private static final String TRUNCATED_MARKER = "...(+100 chars)";
  private static final String FAILED_TO_STRING_VALUE =
      "[LogSanitizerUnitTests.FailingToStringValue, toString failed: IllegalStateException]";

  @Test
  void testSanitize_shouldReturnText_whenValueIsShortAndSingleLine() {
    // When
    String result = LogSanitizer.sanitize(PLAIN_VALUE);

    // Then
    assertEquals(PLAIN_VALUE, result);
  }

  @Test
  void testSanitize_shouldReturnNullText_whenValueIsNull() {
    // When
    String result = LogSanitizer.sanitize(null);

    // Then
    assertEquals(NULL_TEXT, result);
  }

  @Test
  void testSanitize_shouldReturnNullText_whenToStringReturnsNull() {
    // When
    String result = LogSanitizer.sanitize(new NullToStringValue());

    // Then
    assertEquals(NULL_TEXT, result);
  }

  @Test
  void testSanitize_shouldReturnPlaceholder_whenToStringFails() {
    // When
    String result = LogSanitizer.sanitize(new FailingToStringValue());

    // Then
    assertEquals(FAILED_TO_STRING_VALUE, result);
  }

  @Test
  void testEscapeLineBreaks_shouldEscapeCarriageReturnAndLineFeed() {
    // When
    String result = LogSanitizer.escapeLineBreaks(MULTILINE_VALUE);

    // Then
    assertEquals(ESCAPED_MULTILINE_VALUE, result);
  }

  @Test
  void testEscapeLineBreaks_shouldReturnNull_whenTextIsNull() {
    // When
    String result = LogSanitizer.escapeLineBreaks(null);

    // Then
    assertNull(result);
  }

  @Test
  void testSanitize_shouldNotTruncate_whenValueHasMaxLength() {
    // Given
    String value = "x".repeat(MAX_VALUE_LENGTH);

    // When
    String result = LogSanitizer.sanitize(value);

    // Then
    assertEquals(value, result);
  }

  @Test
  void testSanitize_shouldTruncate_whenValueIsLongerThanMaxLength() {
    // When
    String result = LogSanitizer.sanitize("x".repeat(LONG_VALUE_LENGTH));

    // Then
    assertEquals("x".repeat(MAX_VALUE_LENGTH) + TRUNCATED_MARKER, result);
  }

  @Test
  void testSanitize_shouldTruncateAfterEscapingLineBreaks() {
    // Given
    int lineFeedCount = MAX_VALUE_LENGTH / 2 + 50;

    // When
    String result = LogSanitizer.sanitize(LINE_FEED.repeat(lineFeedCount));

    // Then
    assertEquals(ESCAPED_LINE_FEED.repeat(MAX_VALUE_LENGTH / 2) + TRUNCATED_MARKER, result);
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
