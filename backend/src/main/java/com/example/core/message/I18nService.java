package com.example.core.message;

import com.example.core.logging.LogExecution;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class I18nService {

  private final MessageSource messageSource;

  @LogExecution(level = Level.DEBUG, logArguments = true, logResult = true)
  public String getMessage(String key) {
    Locale locale = LocaleContextHolder.getLocale();

    return messageSource.getMessage(key, null, locale);
  }
}
