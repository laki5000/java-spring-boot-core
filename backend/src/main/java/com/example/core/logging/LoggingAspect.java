package com.example.core.logging;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

  private static final String STARTED = " started";
  private static final String COMPLETED_IN = " completed in ";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = " | arguments=";
  private static final String RESULT_PREFIX = " | result=";
  private static final String REDACTED = "[REDACTED]";

  @Around("@annotation(logExecution)")
  public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution)
      throws Throwable {

    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    logStarted(logExecution, className, methodName, joinPoint);

    long start = System.currentTimeMillis();

    Object result = joinPoint.proceed();

    long duration = System.currentTimeMillis() - start;

    logCompleted(logExecution, className, methodName, result, duration);

    return result;
  }

  private void logStarted(
      LogExecution annotation, String className, String methodName, ProceedingJoinPoint joinPoint) {

    String message =
        className
            + "."
            + methodName
            + STARTED
            + (annotation.logArguments()
                ? ARGUMENTS_PREFIX + getArgumentsToLog(joinPoint, annotation)
                : "");

    log(annotation.level(), message);
  }

  private void logCompleted(
      LogExecution annotation, String className, String methodName, Object result, long duration) {

    String message =
        className
            + "."
            + methodName
            + COMPLETED_IN
            + duration
            + MILLISECONDS_SUFFIX
            + (annotation.logResult() ? RESULT_PREFIX + result : "");

    log(annotation.level(), message);
  }

  private String getArgumentsToLog(ProceedingJoinPoint joinPoint, LogExecution annotation) {
    Object[] arguments = joinPoint.getArgs();
    String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();

    int[] indexes = annotation.argumentIndexes();

    return IntStream.range(0, arguments.length)
        .mapToObj(
            index -> {
              boolean shouldLogValue =
                  indexes.length == 0 || Arrays.stream(indexes).anyMatch(i -> i == index);

              Object value = shouldLogValue ? arguments[index] : REDACTED;

              return formatArgument(index, parameterNames[index], value);
            })
        .collect(Collectors.joining(", ", "{", "}"));
  }

  private String formatArgument(int index, String name, Object value) {
    return index + "=" + name + ": " + value;
  }

  private void log(Level level, String message) {
    switch (level) {
      case DEBUG -> log.debug(message);
      case INFO -> log.info(message);
      default -> throw new IllegalArgumentException("Unsupported logging level: " + level);
    }
  }
}
