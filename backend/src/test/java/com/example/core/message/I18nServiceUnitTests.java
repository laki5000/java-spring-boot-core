package com.example.core.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class I18nServiceUnitTests {

  private static final String MESSAGE_KEY = "test.key";
  private static final String EXPECTED_MESSAGE = "Test message";

  @Mock private MessageSource messageSource;

  private I18nService i18nService;

  @BeforeEach
  void setUp() {
    i18nService = new I18nService(messageSource);
  }

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void testGetMessage_shouldReturnLocalizedMessage() {
    // Given
    Locale locale = Locale.ENGLISH;
    LocaleContextHolder.setLocale(locale);

    when(messageSource.getMessage(MESSAGE_KEY, null, locale)).thenReturn(EXPECTED_MESSAGE);

    // When
    String result = i18nService.getMessage(MESSAGE_KEY);

    // Then
    assertEquals(EXPECTED_MESSAGE, result);
    verify(messageSource).getMessage(MESSAGE_KEY, null, locale);
  }
}
