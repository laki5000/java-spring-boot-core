package com.example.core.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.support.AopUtils;

class LoggingAspectUnitTests {

  private static final String EXPECTED_RESULT = "result";
  private static final String EXCEPTION_MESSAGE = "test";

  private static final Long ARGUMENT_1 = 1L;
  private static final String ARGUMENT_2 = "test";
  private static final Boolean ARGUMENT_3 = true;

  private static final String EXPECTED_ARGUMENT_1 = "0=first: 1";
  private static final String EXPECTED_ARGUMENT_2 = "1=second: test";
  private static final String EXPECTED_ARGUMENT_3 = "2=third: true";
  private static final String REDACTED_ARGUMENT_2 = "1=second: [REDACTED]";

  private static final String REDACTED_VALUE = "1=value: [REDACTED]";
  private static final String PLAIN_VALUE = "1=value: test";
  private static final int MAX_VALUE_LENGTH = 500;
  private static final int LONG_VALUE_LENGTH = 600;
  private static final String TRUNCATED_MARKER = "...(+100 chars)";
  private static final String TRUNCATED_PREFIX = "...(+";
  private static final String TRUNCATED_SUFFIX = " chars)";
  private static final String CARRIAGE_RETURN = "\r";
  private static final String LINE_FEED = "\n";
  private static final String ESCAPED_LINE_FEED = "\\n";
  private static final int LINE_FEED_COUNT = 300;
  private static final String MULTILINE_VALUE = "Bob\r\nINFO fake log line";
  private static final String ESCAPED_MULTILINE_VALUE = "Bob\\r\\nINFO fake log line";
  private static final String NULL_TEXT = "null";
  private static final String FAILED_TO_STRING_VALUE =
      "[LoggingAspectUnitTests.FailingToStringValue, toString failed: IllegalStateException]";
  private static final String FAILED_TO_STRING_EXCEPTION =
      "[LoggingAspectUnitTests.FailingToStringException, toString failed: IllegalStateException]";

  private static final String STARTED = " started";
  private static final String COMPLETED_IN = " completed in ";
  private static final String FAILED_AFTER = " failed after ";
  private static final String EXCEPTION_PREFIX = "exception=";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = "arguments=";
  private static final String RESULT_PREFIX = "result=";
  private static final String SECOND_ARGUMENT_PREFIX = "1=second: ";

  private SampleService target;
  private SampleService sampleService;

  private Logger logger;
  private Level originalLevel;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    target = new SampleService();
    sampleService = createProxy(target);

    logger = (Logger) LoggerFactory.getLogger(LoggingAspectUnitTests.class);
    originalLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);

    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(logAppender);
    logger.setLevel(originalLevel);
  }

  @Test
  void testLogExecution_shouldReturnResultAndLogStartedAndCompleted() {
    // When
    Object result = sampleService.plain();

    // Then
    assertEquals(EXPECTED_RESULT, result);
    assertEquals(1, target.invocations);

    assertEquals(2, logAppender.list.size());
    assertStartedLog(logAppender.list.getFirst(), SampleService.PLAIN);
    assertCompletedLog(logAppender.list.getLast(), SampleService.PLAIN);
  }

  @Test
  void testLogExecution_shouldNotLog_whenMethodIsNotAnnotated() {
    // When
    Object result = sampleService.notAnnotated();

    // Then
    assertEquals(EXPECTED_RESULT, result);
    assertTrue(logAppender.list.isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"DEBUG, DEBUG, 2", "DEBUG, INFO, 0", "INFO, INFO, 2", "INFO, WARN, 0"})
  void testLogExecution_shouldLogOnlyWhenAnnotationLevelIsEnabled(
      LogLevel annotationLevel, String loggerLevel, int expectedLogCount) {
    // Given
    Level level = Level.toLevel(loggerLevel, null);
    assertNotNull(level);
    logger.setLevel(level);

    // When
    Object result = annotationLevel == LogLevel.INFO ? sampleService.info() : sampleService.plain();

    // Then
    assertEquals(EXPECTED_RESULT, result);
    assertEquals(expectedLogCount, logAppender.list.size());
    logAppender.list.forEach(
        event -> assertEquals(Level.toLevel(annotationLevel.name()), event.getLevel()));
  }

  @Test
  void testLogExecution_shouldNotFormatArgumentsAndResult_whenLogLevelIsDisabled() {
    // Given
    logger.setLevel(Level.INFO);
    AtomicInteger toStringCalls = new AtomicInteger();
    Object countingObject =
        new Object() {
          @Override
          public String toString() {
            toStringCalls.incrementAndGet();
            return ARGUMENT_2;
          }
        };

    // When
    Object result = sampleService.echo(ARGUMENT_1, countingObject, ARGUMENT_3);

    // Then
    assertSame(countingObject, result);
    assertEquals(1, target.invocations);
    assertTrue(logAppender.list.isEmpty());
    assertEquals(0, toStringCalls.get());
  }

  @Test
  void testLogExecution_shouldLogWithTargetClassLogger() {
    // When
    sampleService.plain();

    // Then
    assertEquals(SampleService.class.getName(), logAppender.list.getFirst().getLoggerName());
  }

  @Test
  void testLogExecution_shouldLogFailedAndPropagateException_whenMethodThrows() {
    // Given
    RuntimeException expectedException = new RuntimeException(EXCEPTION_MESSAGE);

    // When
    RuntimeException actualException =
        assertThrows(RuntimeException.class, () -> sampleService.fail(expectedException));

    // Then
    assertSame(expectedException, actualException);

    assertEquals(2, logAppender.list.size());
    assertStartedLog(logAppender.list.getFirst(), SampleService.FAIL);

    String failedMessage = getLogMessage(1);

    assertTrue(failedMessage.startsWith(SampleService.FAIL + FAILED_AFTER));
    assertTrue(failedMessage.contains(MILLISECONDS_SUFFIX));
    assertTrue(failedMessage.contains(EXCEPTION_PREFIX + expectedException));
    assertEquals(Level.DEBUG, logAppender.list.get(1).getLevel());
    assertNull(logAppender.list.get(1).getThrowableProxy());
  }

  @Test
  void testLogExecution_shouldLogAllArguments_whenArgumentLoggingIsEnabled() {
    // When
    sampleService.withArguments(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertTrue(startedMessage.contains(ARGUMENTS_PREFIX));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_1));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_2));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_3));

    assertFalse(completedMessage.contains(ARGUMENTS_PREFIX));
    assertFalse(completedMessage.contains(RESULT_PREFIX));
  }

  @Test
  void testLogExecution_shouldRedactArgument_whenParameterIsAnnotatedWithRedacted() {
    // When
    sampleService.withRedactedArgument(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3);

    // Then
    String message = getLogMessage(0);

    assertTrue(message.contains(EXPECTED_ARGUMENT_1));
    assertTrue(message.contains(EXPECTED_ARGUMENT_3));
    assertTrue(message.contains(REDACTED_ARGUMENT_2));

    assertFalse(message.contains(EXPECTED_ARGUMENT_2));
  }

  @Test
  void testLogExecution_shouldRedactArgument_whenAnnotationIsOnImplementationOfInterfaceMethod() {
    // Given
    SampleInterface<String> sampleInterface = createProxy(new SampleImplementation());
    assertTrue(AopUtils.isJdkDynamicProxy(sampleInterface));

    // When
    sampleInterface.process(ARGUMENT_2, ARGUMENT_2);

    // Then
    String message = getLogMessage(0);

    assertTrue(message.contains(REDACTED_VALUE));
    assertFalse(message.contains(PLAIN_VALUE));
  }

  @Test
  void testLogExecution_shouldTruncateLongArgumentsAndResult() {
    // Given
    String longValue = "x".repeat(LONG_VALUE_LENGTH);

    // When
    sampleService.echo(ARGUMENT_1, longValue, ARGUMENT_3);

    // Then
    String truncatedValue = "x".repeat(MAX_VALUE_LENGTH) + TRUNCATED_MARKER;

    assertTrue(getLogMessage(0).contains(SECOND_ARGUMENT_PREFIX + truncatedValue));
    assertTrue(getLogMessage(1).contains(RESULT_PREFIX + truncatedValue));
    assertFalse(getLogMessage(1).contains(longValue));
  }

  @Test
  void testLogExecution_shouldTruncateLongExceptionMessage() {
    // Given
    RuntimeException exception = new RuntimeException("x".repeat(LONG_VALUE_LENGTH));

    // When
    assertThrows(RuntimeException.class, () -> sampleService.fail(exception));

    // Then
    String failedMessage = getLogMessage(1);
    String exceptionText = exception.toString();
    String truncatedValue =
        exceptionText.substring(0, MAX_VALUE_LENGTH)
            + TRUNCATED_PREFIX
            + (exceptionText.length() - MAX_VALUE_LENGTH)
            + TRUNCATED_SUFFIX;

    assertTrue(failedMessage.endsWith(EXCEPTION_PREFIX + truncatedValue));
  }

  @Test
  void testLogExecution_shouldEscapeLineBreaksInArgumentsAndResult() {
    // When
    sampleService.echo(ARGUMENT_1, MULTILINE_VALUE, ARGUMENT_3);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertTrue(startedMessage.contains(SECOND_ARGUMENT_PREFIX + ESCAPED_MULTILINE_VALUE));
    assertTrue(completedMessage.contains(RESULT_PREFIX + ESCAPED_MULTILINE_VALUE));
    assertFalse(startedMessage.contains(LINE_FEED) || startedMessage.contains(CARRIAGE_RETURN));
    assertFalse(completedMessage.contains(LINE_FEED) || completedMessage.contains(CARRIAGE_RETURN));
  }

  @Test
  void testLogExecution_shouldTruncateAfterEscapingLineBreaks() {
    // When
    sampleService.echo(ARGUMENT_1, LINE_FEED.repeat(LINE_FEED_COUNT), ARGUMENT_3);

    // Then
    String escapedValue = ESCAPED_LINE_FEED.repeat(LINE_FEED_COUNT);
    String truncatedValue =
        escapedValue.substring(0, MAX_VALUE_LENGTH)
            + TRUNCATED_PREFIX
            + (escapedValue.length() - MAX_VALUE_LENGTH)
            + TRUNCATED_SUFFIX;

    assertTrue(getLogMessage(1).endsWith(RESULT_PREFIX + truncatedValue));
  }

  @Test
  void testLogExecution_shouldEscapeLineBreaksInExceptionMessage() {
    // When
    assertThrows(
        RuntimeException.class, () -> sampleService.fail(new RuntimeException(MULTILINE_VALUE)));

    // Then
    String failedMessage = getLogMessage(1);

    assertTrue(failedMessage.contains(ESCAPED_MULTILINE_VALUE));
    assertFalse(failedMessage.contains(LINE_FEED) || failedMessage.contains(CARRIAGE_RETURN));
  }

  @Test
  void testLogExecution_shouldKeepResult_whenArgumentOrResultToStringFails() {
    // Given
    FailingToStringValue failingValue = new FailingToStringValue();

    // When
    Object result = sampleService.echo(ARGUMENT_1, failingValue, ARGUMENT_3);

    // Then
    assertSame(failingValue, result);
    assertTrue(getLogMessage(0).contains(SECOND_ARGUMENT_PREFIX + FAILED_TO_STRING_VALUE));
    assertTrue(getLogMessage(1).contains(RESULT_PREFIX + FAILED_TO_STRING_VALUE));
  }

  @Test
  void testLogExecution_shouldKeepResult_whenArgumentOrResultToStringReturnsNull() {
    // Given
    NullToStringValue nullValue = new NullToStringValue();

    // When
    Object result = sampleService.echo(ARGUMENT_1, nullValue, ARGUMENT_3);

    // Then
    assertSame(nullValue, result);
    assertTrue(getLogMessage(0).contains(SECOND_ARGUMENT_PREFIX + NULL_TEXT));
    assertTrue(getLogMessage(1).endsWith(RESULT_PREFIX + NULL_TEXT));
  }

  @Test
  void testLogExecution_shouldPropagateOriginalException_whenExceptionToStringFails() {
    // Given
    FailingToStringException expectedException = new FailingToStringException();

    // When
    FailingToStringException actualException =
        assertThrows(FailingToStringException.class, () -> sampleService.fail(expectedException));

    // Then
    assertSame(expectedException, actualException);
    assertTrue(getLogMessage(1).endsWith(EXCEPTION_PREFIX + FAILED_TO_STRING_EXCEPTION));
  }

  @Test
  void testLogExecution_shouldNotLogArguments_whenArgumentLoggingIsDisabled() {
    // When
    sampleService.withoutArgumentLogging(ARGUMENT_1);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertFalse(startedMessage.contains(ARGUMENTS_PREFIX));
    assertFalse(startedMessage.contains(EXPECTED_ARGUMENT_1));

    assertFalse(completedMessage.contains(ARGUMENTS_PREFIX));
  }

  @Test
  void testLogExecution_shouldLogResult_whenResultLoggingIsEnabled() {
    // When
    sampleService.withResult();

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertFalse(startedMessage.contains(RESULT_PREFIX));
    assertTrue(completedMessage.contains(RESULT_PREFIX + EXPECTED_RESULT));
  }

  private static <T> T createProxy(Object target) {
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(new LoggingAspect());

    return factory.getProxy();
  }

  private String getLogMessage(int index) {
    List<ILoggingEvent> logs = logAppender.list;

    assertFalse(logs.isEmpty());

    return logs.get(index).getFormattedMessage();
  }

  private void assertStartedLog(ILoggingEvent log, String methodName) {
    String message = log.getFormattedMessage();

    assertTrue(message.startsWith(methodName + STARTED));
    assertFalse(message.contains(RESULT_PREFIX));
  }

  private void assertCompletedLog(ILoggingEvent log, String methodName) {
    String message = log.getFormattedMessage();

    assertTrue(message.startsWith(methodName + COMPLETED_IN));
    assertTrue(message.contains(MILLISECONDS_SUFFIX));
  }

  static class SampleService {

    static final String PLAIN = "plain";
    static final String FAIL = "fail";

    int invocations;

    @LogExecution
    public Object plain() {
      invocations++;
      return EXPECTED_RESULT;
    }

    @LogExecution(level = LogLevel.INFO)
    public Object info() {
      return EXPECTED_RESULT;
    }

    public Object notAnnotated() {
      return EXPECTED_RESULT;
    }

    @LogExecution(logArguments = true)
    public void withArguments(Long first, String second, Boolean third) {}

    @LogExecution(logArguments = true)
    public void withRedactedArgument(Long first, @Redacted String second, Boolean third) {}

    @LogExecution
    public void withoutArgumentLogging(Long first) {}

    @LogExecution(logResult = true)
    public Object withResult() {
      return EXPECTED_RESULT;
    }

    @LogExecution(logArguments = true, logResult = true)
    public Object echo(Long first, Object second, Boolean third) {
      invocations++;
      return second;
    }

    @LogExecution
    public void fail(RuntimeException exception) {
      throw exception;
    }
  }

  interface SampleInterface<T> {
    void process(String name, T value);
  }

  static class SampleImplementation implements SampleInterface<String> {
    @Override
    @LogExecution(logArguments = true)
    public void process(String name, @Redacted String value) {}
  }

  static class FailingToStringValue {
    @Override
    public String toString() {
      throw new IllegalStateException();
    }
  }

  static class NullToStringValue {
    @Override
    public String toString() {
      return null;
    }
  }

  static class FailingToStringException extends RuntimeException {
    @Override
    public String toString() {
      throw new IllegalStateException();
    }
  }
}
