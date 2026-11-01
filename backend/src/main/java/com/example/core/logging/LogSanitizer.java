package com.example.core.logging;

import java.util.Objects;
import org.springframework.util.ClassUtils;

public final class LogSanitizer {

  private static final int MAX_VALUE_LENGTH = 500;
  private static final String TRUNCATED_PREFIX = "...(+";
  private static final String TRUNCATED_SUFFIX = " chars)";
  private static final String CARRIAGE_RETURN = "\r";
  private static final String ESCAPED_CARRIAGE_RETURN = "\\r";
  private static final String LINE_FEED = "\n";
  private static final String ESCAPED_LINE_FEED = "\\n";
  private static final String NULL_TEXT = "null";
  private static final String TO_STRING_FAILED_PREFIX = "[";
  private static final String TO_STRING_FAILED_SEPARATOR = ", toString failed: ";
  private static final String TO_STRING_FAILED_SUFFIX = "]";

  private LogSanitizer() {}

  public static String sanitize(Object value) {
    String text = escapeLineBreaks(toText(value));

    if (text.length() <= MAX_VALUE_LENGTH) {
      return text;
    }

    return text.substring(0, MAX_VALUE_LENGTH)
        + TRUNCATED_PREFIX
        + (text.length() - MAX_VALUE_LENGTH)
        + TRUNCATED_SUFFIX;
  }

  public static String escapeLineBreaks(String text) {
    if (text == null) {
      return null;
    }

    return text.replace(CARRIAGE_RETURN, ESCAPED_CARRIAGE_RETURN)
        .replace(LINE_FEED, ESCAPED_LINE_FEED);
  }

  private static String toText(Object value) {
    try {
      return Objects.requireNonNullElse(String.valueOf(value), NULL_TEXT);
    } catch (RuntimeException exception) {
      return TO_STRING_FAILED_PREFIX
          + ClassUtils.getShortName(ClassUtils.getUserClass(value))
          + TO_STRING_FAILED_SEPARATOR
          + ClassUtils.getShortName(exception.getClass())
          + TO_STRING_FAILED_SUFFIX;
    }
  }
}
