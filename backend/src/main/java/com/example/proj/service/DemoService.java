package com.example.proj.service;

import com.example.core.logging.LogExecution;
import com.example.core.message.I18nService;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoService {

  private static final String DEMO_RUNNING_MESSAGE = "demo.running";
  private static final String DEMO_EXAMPLE_EXCEPTION_MESSAGE = "demo.example.exception";

  private final I18nService i18nService;

  @LogExecution(level = Level.INFO, logArguments = true, logResult = true)
  public String getDemoMessage(Boolean error) {
    if (Boolean.TRUE.equals(error)) {
      throw new RuntimeException(i18nService.getMessage(DEMO_EXAMPLE_EXCEPTION_MESSAGE));
    }

    return i18nService.getMessage(DEMO_RUNNING_MESSAGE);
  }
}
