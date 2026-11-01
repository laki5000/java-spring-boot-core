package com.example.core.logging;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Aspect
@Component
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

    Logger logger = LoggerFactory.getLogger(ClassUtils.getUserClass(joinPoint.getTarget()));
    Level level = toSlf4jLevel(logExecution.level());

    if (!logger.isEnabledForLevel(level)) {
      return joinPoint.proceed();
    }

    String methodName = joinPoint.getSignature().getName();

    logStarted(logger, level, logExecution, methodName, joinPoint);

    long start = System.currentTimeMillis();

    Object result = joinPoint.proceed();

    long duration = System.currentTimeMillis() - start;

    logCompleted(logger, level, logExecution, methodName, result, duration);

    return result;
  }

  private void logStarted(
      Logger logger,
      Level level,
      LogExecution annotation,
      String methodName,
      ProceedingJoinPoint joinPoint) {

    String message =
        methodName
            + STARTED
            + (annotation.logArguments()
                ? ARGUMENTS_PREFIX + getArgumentsToLog(joinPoint, annotation)
                : "");

    logger.atLevel(level).log(message);
  }

  private void logCompleted(
      Logger logger,
      Level level,
      LogExecution annotation,
      String methodName,
      Object result,
      long duration) {

    String message =
        methodName
            + COMPLETED_IN
            + duration
            + MILLISECONDS_SUFFIX
            + (annotation.logResult() ? RESULT_PREFIX + result : "");

    logger.atLevel(level).log(message);
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

  private Level toSlf4jLevel(LogLevel level) {
    return switch (level) {
      case DEBUG -> Level.DEBUG;
      case INFO -> Level.INFO;
    };
  }
}
