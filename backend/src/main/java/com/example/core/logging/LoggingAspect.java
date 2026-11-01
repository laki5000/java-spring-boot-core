package com.example.core.logging;

import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Aspect
@Component
public class LoggingAspect {

  private static final String STARTED = " started";
  private static final String COMPLETED_IN = " completed in ";
  private static final String FAILED_AFTER = " failed after ";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = " | arguments=";
  private static final String RESULT_PREFIX = " | result=";
  private static final String EXCEPTION_PREFIX = " | exception=";
  private static final String REDACTED = "[REDACTED]";
  private static final int MAX_VALUE_LENGTH = 500;
  private static final String TRUNCATED_PREFIX = "...(+";
  private static final String TRUNCATED_SUFFIX = " chars)";
  private static final String CARRIAGE_RETURN = "\r";
  private static final String ESCAPED_CARRIAGE_RETURN = "\\r";
  private static final String LINE_FEED = "\n";
  private static final String ESCAPED_LINE_FEED = "\\n";

  @Around("@annotation(logExecution)")
  public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution)
      throws Throwable {

    Class<?> targetClass = ClassUtils.getUserClass(joinPoint.getTarget());
    Logger logger = LoggerFactory.getLogger(targetClass);
    Level level = toSlf4jLevel(logExecution.level());

    if (!logger.isEnabledForLevel(level)) {
      return joinPoint.proceed();
    }

    String methodName = joinPoint.getSignature().getName();

    logStarted(logger, level, logExecution, methodName, joinPoint, targetClass);

    long start = System.nanoTime();

    Object result;

    try {
      result = joinPoint.proceed();
    } catch (Throwable throwable) {
      logFailed(logger, level, methodName, throwable, getElapsedMillis(start));
      throw throwable;
    }

    long duration = getElapsedMillis(start);

    logCompleted(logger, level, logExecution, methodName, result, duration);

    return result;
  }

  private void logStarted(
      Logger logger,
      Level level,
      LogExecution annotation,
      String methodName,
      ProceedingJoinPoint joinPoint,
      Class<?> targetClass) {

    String message =
        methodName
            + STARTED
            + (annotation.logArguments()
                ? ARGUMENTS_PREFIX + getArgumentsToLog(joinPoint, targetClass)
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
            + (annotation.logResult() ? RESULT_PREFIX + formatValue(result) : "");

    logger.atLevel(level).log(message);
  }

  private void logFailed(
      Logger logger, Level level, String methodName, Throwable throwable, long duration) {

    String message =
        methodName
            + FAILED_AFTER
            + duration
            + MILLISECONDS_SUFFIX
            + EXCEPTION_PREFIX
            + formatValue(throwable);

    logger.atLevel(level).log(message);
  }

  private String getArgumentsToLog(ProceedingJoinPoint joinPoint, Class<?> targetClass) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Object[] arguments = joinPoint.getArgs();
    Parameter[] parameters =
        AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass).getParameters();

    return IntStream.range(0, arguments.length)
        .mapToObj(
            index -> {
              Object value =
                  parameters[index].isAnnotationPresent(Redacted.class)
                      ? REDACTED
                      : arguments[index];

              return formatArgument(index, parameters[index].getName(), value);
            })
        .collect(Collectors.joining(", ", "{", "}"));
  }

  private String formatArgument(int index, String name, Object value) {
    return index + "=" + name + ": " + formatValue(value);
  }

  private String formatValue(Object value) {
    String text =
        String.valueOf(value)
            .replace(CARRIAGE_RETURN, ESCAPED_CARRIAGE_RETURN)
            .replace(LINE_FEED, ESCAPED_LINE_FEED);

    if (text.length() <= MAX_VALUE_LENGTH) {
      return text;
    }

    return text.substring(0, MAX_VALUE_LENGTH)
        + TRUNCATED_PREFIX
        + (text.length() - MAX_VALUE_LENGTH)
        + TRUNCATED_SUFFIX;
  }

  private long getElapsedMillis(long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }

  private Level toSlf4jLevel(LogLevel level) {
    return switch (level) {
      case DEBUG -> Level.DEBUG;
      case INFO -> Level.INFO;
    };
  }
}
