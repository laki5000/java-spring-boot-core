package com.example.core.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;

@SpringBootTest(
    classes = I18nService.class,
    properties = "spring.messages.basename=messages/test-messages,messages/messages")
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
class MessageSourceIntegrationTests {

  private static final String MESSAGE_KEY = "test.i18n.message";
  private static final String MISSING_MESSAGE_KEY = "missing.i18n.message";
  private static final String EXPECTED_ENGLISH_MESSAGE = "Test English message";

  @Autowired private I18nService i18nService;

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void getMessage_shouldReturnEnglishMessage_whenLocaleIsUnknown() {
    // Given
    LocaleContextHolder.setLocale(Locale.of("de"));

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertThat(result).isEqualTo(EXPECTED_ENGLISH_MESSAGE);
  }

  @Test
  void getMessage_shouldReturnMessageKey_whenMessageKeyDoesNotExist() {
    // Given
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    // When
    String result = i18nService.getMessage(MISSING_MESSAGE_KEY);

    // Then
    assertThat(result).isEqualTo(MISSING_MESSAGE_KEY);
  }

  @Test
  void getMessage_shouldReturnHungarianMessage_whenLocaleIsHungarian() {
    // Given
    LocaleContextHolder.setLocale(Locale.forLanguageTag("hu"));

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertThat(result).isEqualTo("Teszt magyar üzenet");
  }

  @Test
  void getMessage_shouldReturnEnglishMessage_whenLocaleIsEnglish() {
    // Given
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertThat(result).isEqualTo(EXPECTED_ENGLISH_MESSAGE);
  }
}
