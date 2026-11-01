package com.example.proj.service;

import com.example.core.logging.LogExecution;
import com.example.core.logging.LogLevel;
import com.example.core.message.I18nService;
import com.example.proj.exception.DemoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoService {

  private static final String DEMO_RUNNING_MESSAGE = "demo.running";
  private static final String DEMO_ERROR_MESSAGE = "Demo error was requested";

  private final I18nService i18nService;

  @LogExecution(level = LogLevel.INFO, logArguments = true, logResult = true)
  public String getDemoMessage(Boolean error) {
    if (Boolean.TRUE.equals(error)) {
      throw new DemoException(DEMO_ERROR_MESSAGE);
    }

    return i18nService.getMessage(DEMO_RUNNING_MESSAGE);
  }
}
