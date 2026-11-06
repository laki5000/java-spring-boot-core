package com.example.core.logging;

import java.util.Objects;
import org.springframework.util.ClassUtils;

public final class LogSanitizer {

  private static final int MAX_VALUE_LENGTH = 500;

  private LogSanitizer() {}

  public static String sanitize(Object value) {
    String text = escapeLineBreaks(toText(value));

    if (text.length() <= MAX_VALUE_LENGTH) {
      return text;
    }

    return "%s...(+%s chars)"
        .formatted(text.substring(0, MAX_VALUE_LENGTH), text.length() - MAX_VALUE_LENGTH);
  }

  public static String escapeLineBreaks(String text) {
    if (text == null) {
      return null;
    }

    return text.replace("\r", "\\r").replace("\n", "\\n");
  }

  private static String toText(Object value) {
    try {
      return Objects.requireNonNullElse(String.valueOf(value), "null");
    } catch (RuntimeException exception) {
      return "[%s, toString failed: %s]"
          .formatted(
              ClassUtils.getShortName(ClassUtils.getUserClass(value)),
              ClassUtils.getShortName(exception.getClass()));
    }
  }
}
