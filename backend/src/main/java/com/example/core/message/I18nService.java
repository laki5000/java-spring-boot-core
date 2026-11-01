package com.example.core.message;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class I18nService {

  private final MessageSource messageSource;

  public String getMessage(String key) {
    Locale locale = LocaleContextHolder.getLocale();

    return messageSource.getMessage(key, null, key, locale);
  }
}
