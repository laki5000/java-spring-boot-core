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

  private static final String REDACTED = "[REDACTED]";

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
            + " started"
            + (annotation.logArguments()
                ? " | arguments=" + getArgumentsToLog(joinPoint, targetClass)
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
        "%s completed in %s ms".formatted(methodName, duration)
            + (annotation.logResult() ? " | result=" + LogSanitizer.sanitize(result) : "");

    logger.atLevel(level).log(message);
  }

  private void logFailed(
      Logger logger, Level level, String methodName, Throwable throwable, long duration) {

    String message =
        "%s failed after %s ms | exception=%s"
            .formatted(methodName, duration, LogSanitizer.sanitize(throwable));

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
    return "%s=%s: %s".formatted(index, name, LogSanitizer.sanitize(value));
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
