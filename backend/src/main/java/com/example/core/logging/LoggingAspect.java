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

  private static final String COMPLETED_IN = " completed in ";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = " | arguments=";
  private static final String RESULT_PREFIX = " | result=";

  @Around("@annotation(logExecution)")
  public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution)
      throws Throwable {

    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    long start = System.currentTimeMillis();

    Object result = joinPoint.proceed();

    long duration = System.currentTimeMillis() - start;

    logSuccess(logExecution, className, methodName, joinPoint, result, duration);

    return result;
  }

  private void logSuccess(
      LogExecution annotation,
      String className,
      String methodName,
      ProceedingJoinPoint joinPoint,
      Object result,
      long duration) {

    String message =
        buildSuccessMessage(annotation, className, methodName, joinPoint, result, duration);

    log(annotation.level(), message);
  }

  private String buildSuccessMessage(
      LogExecution annotation,
      String className,
      String methodName,
      ProceedingJoinPoint joinPoint,
      Object result,
      long duration) {

    StringBuilder message =
        new StringBuilder()
            .append(className)
            .append(".")
            .append(methodName)
            .append(COMPLETED_IN)
            .append(duration)
            .append(MILLISECONDS_SUFFIX);

    if (annotation.logArguments()) {
      message.append(ARGUMENTS_PREFIX).append(getArgumentsToLog(joinPoint, annotation));
    }

    if (annotation.logResult()) {
      message.append(RESULT_PREFIX).append(result);
    }

    return message.toString();
  }

  private String getArgumentsToLog(ProceedingJoinPoint joinPoint, LogExecution annotation) {
    Object[] arguments = joinPoint.getArgs();
    String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();

    int[] indexes = annotation.argumentIndexes();

    if (indexes.length == 0) {
      return IntStream.range(0, arguments.length)
          .mapToObj(index -> formatArgument(index, parameterNames[index], arguments[index]))
          .collect(Collectors.joining(", ", "{", "}"));
    }

    return Arrays.stream(indexes)
        .filter(index -> index >= 0 && index < arguments.length)
        .mapToObj(index -> formatArgument(index, parameterNames[index], arguments[index]))
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
