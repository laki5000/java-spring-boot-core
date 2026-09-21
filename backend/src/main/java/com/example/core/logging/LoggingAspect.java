package com.example.core.logging;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

  @Around("@annotation(logExecution)")
  public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution)
      throws Throwable {

    String className = joinPoint.getTarget().getClass().getSimpleName();

    String methodName = joinPoint.getSignature().getName();

    long start = System.currentTimeMillis();

    Object result = joinPoint.proceed();

    long duration = System.currentTimeMillis() - start;

    logSuccess(logExecution, className, methodName, joinPoint.getArgs(), result, duration);

    return result;
  }

  private void logSuccess(
      LogExecution annotation,
      String className,
      String methodName,
      Object[] arguments,
      Object result,
      long duration) {
    String message =
        buildSuccessMessage(annotation, className, methodName, arguments, result, duration);

    log(annotation.level(), message);
  }

  private String buildSuccessMessage(
      LogExecution annotation,
      String className,
      String methodName,
      Object[] arguments,
      Object result,
      long duration) {

    StringBuilder message =
        new StringBuilder()
            .append(className)
            .append(".")
            .append(methodName)
            .append(" completed in ")
            .append(duration)
            .append(" ms");

    if (annotation.logArguments()) {
      message.append(" | arguments=").append(getArgumentsToLog(annotation, arguments));
    }

    if (annotation.logResult()) {
      message.append(" | result=").append(result);
    }

    return message.toString();
  }

  private String getArgumentsToLog(LogExecution annotation, Object[] arguments) {
    int[] indexes = annotation.argumentIndexes();

    if (indexes.length == 0) {
      return Arrays.toString(arguments);
    }

    return Arrays.stream(indexes)
        .filter(index -> index >= 0 && index < arguments.length)
        .mapToObj(index -> arguments[index])
        .toList()
        .toString();
  }

  private void log(Level level, String message) {
    switch (level) {
      case DEBUG -> log.debug(message);
      case INFO -> log.info(message);
      default -> throw new IllegalArgumentException("Unsupported logging level: " + level);
    }
  }
}
