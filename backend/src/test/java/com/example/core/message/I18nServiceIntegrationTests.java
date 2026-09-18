package com.example.core.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;

@SpringBootTest
class I18nServiceIntegrationTests {

  private static final String MESSAGE_KEY = "test.i18n.message";
  private static final String MISSING_MESSAGE_KEY = "missing.i18n.message";
  private static final String UNKNOWN_LOCALE = "de";
  private static final String EXPECTED_ENGLISH_MESSAGE = "Test English message";
  private static final String EXPECTED_HUNGARIAN_MESSAGE = "Teszt magyar üzenet";

  @Autowired private I18nService i18nService;

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void testGetMessage_shouldReturnMessageKey_whenLocaleIsUnknown() {
    // Given
    LocaleContextHolder.setLocale(Locale.of(UNKNOWN_LOCALE));

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertEquals(MESSAGE_KEY, result);
  }

  @Test
  void testGetMessage_shouldReturnMessageKey_whenMessageKeyDoesNotExist() {
    // Given
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    // When
    String result = i18nService.getMessage(MISSING_MESSAGE_KEY);

    // Then
    assertEquals(MISSING_MESSAGE_KEY, result);
  }

  @Test
  void testGetMessage_shouldReturnHungarianMessage_whenLocaleIsHungarian() {
    // Given
    LocaleContextHolder.setLocale(Locale.forLanguageTag("hu"));

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertEquals(EXPECTED_HUNGARIAN_MESSAGE, result);
  }

  @Test
  void testGetMessage_shouldReturnEnglishMessage_whenLocaleIsEnglish() {
    // Given
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertEquals(EXPECTED_ENGLISH_MESSAGE, result);
  }
}
