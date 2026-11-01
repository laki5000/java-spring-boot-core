package com.example.core.logging;

import static com.example.core.logging.LogLevel.DEBUG;
import static com.example.core.logging.LogLevel.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class LoggingAspectUnitTests {

  private static final String METHOD_NAME = "testMethod";
  private static final String EXPECTED_RESULT = "result";
  private static final String EXCEPTION_MESSAGE = "test";

  private static final Long ARGUMENT_1 = 1L;
  private static final String ARGUMENT_2 = "test";
  private static final Boolean ARGUMENT_3 = true;

  private static final String EXPECTED_ARGUMENT_1 = "0=first: 1";
  private static final String EXPECTED_ARGUMENT_2 = "1=second: test";
  private static final String EXPECTED_ARGUMENT_3 = "2=third: true";
  private static final String REDACTED_ARGUMENT_2 = "1=second: [REDACTED]";

  private static final String SAMPLE_METHOD = "sampleMethod";
  private static final String REDACTED_SAMPLE_METHOD = "redactedSampleMethod";
  private static final String INTERFACE_METHOD = "process";
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

  private static final String STARTED = "testMethod started";
  private static final String COMPLETED_PREFIX = "testMethod completed in ";
  private static final String FAILED_PREFIX = "testMethod failed after ";
  private static final String EXCEPTION_PREFIX = "exception=";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = "arguments=";
  private static final String RESULT_PREFIX = "result=";

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private MethodSignature signature;

  @Mock private LogExecution logExecution;

  private LoggingAspect loggingAspect;

  private Logger logger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    loggingAspect = new LoggingAspect();

    logger = (Logger) LoggerFactory.getLogger(LoggingAspectUnitTests.class);
    logger.setLevel(Level.DEBUG);

    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    when(joinPoint.getTarget()).thenReturn(this);
    lenient().when(joinPoint.getSignature()).thenReturn(signature);
    lenient().when(signature.getName()).thenReturn(METHOD_NAME);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(logAppender);
  }

  @Test
  void testLogExecution_shouldReturnResultFromProceed() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();

    assertEquals(2, logAppender.list.size());
    assertStartedLog(logAppender.list.getFirst());
    assertCompletedLog(logAppender.list.getLast());
  }

  @Test
  void testLogExecution_shouldNotFormatArgumentsAndResult_whenLogLevelIsDisabled()
      throws Throwable {
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

    when(joinPoint.proceed()).thenReturn(countingObject);
    when(logExecution.level()).thenReturn(DEBUG);
    lenient().when(logExecution.logArguments()).thenReturn(true);
    lenient().when(logExecution.logResult()).thenReturn(true);
    lenient()
        .when(joinPoint.getArgs())
        .thenReturn(new Object[] {countingObject, countingObject, countingObject});
    lenient().when(signature.getMethod()).thenReturn(getSampleMethod(SAMPLE_METHOD));

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(countingObject, result);
    assertTrue(logAppender.list.isEmpty());
    assertEquals(0, toStringCalls.get());
  }

  @Test
  void testLogExecution_shouldLogWithTargetClassLogger() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(
        LoggingAspectUnitTests.class.getName(), logAppender.list.getFirst().getLoggerName());
  }

  @Test
  void testLogExecution_shouldLogFailedAndPropagateException_whenProceedThrows() throws Throwable {
    // Given
    RuntimeException expectedException = new RuntimeException(EXCEPTION_MESSAGE);

    when(joinPoint.proceed()).thenThrow(expectedException);
    when(logExecution.level()).thenReturn(DEBUG);

    // When / Then
    RuntimeException actualException =
        assertThrows(
            RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

    assertSame(expectedException, actualException);
    verify(joinPoint).proceed();

    assertEquals(2, logAppender.list.size());
    assertStartedLog(logAppender.list.getFirst());

    String failedMessage = getLogMessage(1);

    assertTrue(failedMessage.contains(FAILED_PREFIX));
    assertTrue(failedMessage.contains(MILLISECONDS_SUFFIX));
    assertTrue(failedMessage.contains(EXCEPTION_PREFIX + expectedException));
    assertEquals(Level.DEBUG, logAppender.list.get(1).getLevel());
    assertNull(logAppender.list.get(1).getThrowableProxy());
  }

  @Test
  void testLogExecution_shouldLogAllArguments_whenArgumentLoggingIsEnabled() throws Throwable {
    // Given
    mockArguments(SAMPLE_METHOD);

    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();

    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertTrue(startedMessage.contains(STARTED));
    assertTrue(startedMessage.contains(ARGUMENTS_PREFIX));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_1));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_2));
    assertTrue(startedMessage.contains(EXPECTED_ARGUMENT_3));

    assertFalse(completedMessage.contains(ARGUMENTS_PREFIX));
    assertFalse(completedMessage.contains(RESULT_PREFIX));
  }

  @Test
  void testLogExecution_shouldRedactArgument_whenParameterIsAnnotatedWithRedacted()
      throws Throwable {
    // Given
    mockArguments(REDACTED_SAMPLE_METHOD);

    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String message = getLogMessage(0);

    assertTrue(message.contains(EXPECTED_ARGUMENT_1));
    assertTrue(message.contains(EXPECTED_ARGUMENT_3));
    assertTrue(message.contains(REDACTED_ARGUMENT_2));

    assertFalse(message.contains(EXPECTED_ARGUMENT_2));
  }

  @Test
  void testLogExecution_shouldRedactArgument_whenAnnotationIsOnImplementationOfInterfaceMethod()
      throws Throwable {
    // Given
    when(joinPoint.getTarget()).thenReturn(new SampleImplementation());
    when(joinPoint.getArgs()).thenReturn(new Object[] {ARGUMENT_2, ARGUMENT_2});
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(signature.getMethod())
        .thenReturn(SampleInterface.class.getMethod(INTERFACE_METHOD, String.class, Object.class));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String message = getLogMessage(0);

    assertTrue(message.contains(REDACTED_VALUE));
    assertFalse(message.contains(PLAIN_VALUE));
  }

  @Test
  void testLogExecution_shouldTruncateLongArgumentsAndResult() throws Throwable {
    // Given
    String longValue = "x".repeat(LONG_VALUE_LENGTH);

    when(joinPoint.getArgs()).thenReturn(new Object[] {ARGUMENT_1, longValue, ARGUMENT_3});
    when(joinPoint.proceed()).thenReturn(longValue);
    when(signature.getMethod()).thenReturn(getSampleMethod(SAMPLE_METHOD));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(true);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String truncatedValue = "x".repeat(MAX_VALUE_LENGTH) + TRUNCATED_MARKER;

    assertTrue(getLogMessage(0).contains("1=second: " + truncatedValue));
    assertTrue(getLogMessage(1).contains(RESULT_PREFIX + truncatedValue));
    assertFalse(getLogMessage(1).contains(longValue));
  }

  @Test
  void testLogExecution_shouldTruncateLongExceptionMessage() throws Throwable {
    // Given
    RuntimeException exception = new RuntimeException("x".repeat(LONG_VALUE_LENGTH));

    when(joinPoint.proceed()).thenThrow(exception);
    when(logExecution.level()).thenReturn(DEBUG);

    // When
    assertThrows(RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

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
  void testLogExecution_shouldEscapeLineBreaksInArgumentsAndResult() throws Throwable {
    // Given
    when(joinPoint.getArgs()).thenReturn(new Object[] {ARGUMENT_1, MULTILINE_VALUE, ARGUMENT_3});
    when(joinPoint.proceed()).thenReturn(MULTILINE_VALUE);
    when(signature.getMethod()).thenReturn(getSampleMethod(SAMPLE_METHOD));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(true);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertTrue(startedMessage.contains("1=second: " + ESCAPED_MULTILINE_VALUE));
    assertTrue(completedMessage.contains(RESULT_PREFIX + ESCAPED_MULTILINE_VALUE));
    assertFalse(startedMessage.contains(LINE_FEED) || startedMessage.contains(CARRIAGE_RETURN));
    assertFalse(completedMessage.contains(LINE_FEED) || completedMessage.contains(CARRIAGE_RETURN));
  }

  @Test
  void testLogExecution_shouldTruncateAfterEscapingLineBreaks() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(LINE_FEED.repeat(LINE_FEED_COUNT));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(true);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

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
  void testLogExecution_shouldEscapeLineBreaksInExceptionMessage() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenThrow(new RuntimeException(MULTILINE_VALUE));
    when(logExecution.level()).thenReturn(DEBUG);

    // When
    assertThrows(RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

    // Then
    String failedMessage = getLogMessage(1);

    assertTrue(failedMessage.contains(ESCAPED_MULTILINE_VALUE));
    assertFalse(failedMessage.contains(LINE_FEED) || failedMessage.contains(CARRIAGE_RETURN));
  }

  @Test
  void testLogExecution_shouldKeepResult_whenArgumentOrResultToStringFails() throws Throwable {
    // Given
    FailingToStringValue failingValue = new FailingToStringValue();

    when(joinPoint.getArgs()).thenReturn(new Object[] {ARGUMENT_1, failingValue, ARGUMENT_3});
    when(joinPoint.proceed()).thenReturn(failingValue);
    when(signature.getMethod()).thenReturn(getSampleMethod(SAMPLE_METHOD));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(true);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertSame(failingValue, result);
    assertTrue(getLogMessage(0).contains("1=second: " + FAILED_TO_STRING_VALUE));
    assertTrue(getLogMessage(1).contains(RESULT_PREFIX + FAILED_TO_STRING_VALUE));
  }

  @Test
  void testLogExecution_shouldKeepResult_whenArgumentOrResultToStringReturnsNull()
      throws Throwable {
    // Given
    NullToStringValue nullValue = new NullToStringValue();

    when(joinPoint.getArgs()).thenReturn(new Object[] {ARGUMENT_1, nullValue, ARGUMENT_3});
    when(joinPoint.proceed()).thenReturn(nullValue);
    when(signature.getMethod()).thenReturn(getSampleMethod(SAMPLE_METHOD));
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.logResult()).thenReturn(true);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertSame(nullValue, result);
    assertTrue(getLogMessage(0).contains("1=second: " + NULL_TEXT));
    assertTrue(getLogMessage(1).endsWith(RESULT_PREFIX + NULL_TEXT));
  }

  @Test
  void testLogExecution_shouldPropagateOriginalException_whenExceptionToStringFails()
      throws Throwable {
    // Given
    FailingToStringException expectedException = new FailingToStringException();

    when(joinPoint.proceed()).thenThrow(expectedException);
    when(logExecution.level()).thenReturn(DEBUG);

    // When
    FailingToStringException actualException =
        assertThrows(
            FailingToStringException.class,
            () -> loggingAspect.logExecution(joinPoint, logExecution));

    // Then
    assertSame(expectedException, actualException);
    assertTrue(getLogMessage(1).endsWith(EXCEPTION_PREFIX + FAILED_TO_STRING_EXCEPTION));
  }

  @Test
  void testLogExecution_shouldNotLogArguments_whenArgumentLoggingIsDisabled() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertFalse(startedMessage.contains(ARGUMENTS_PREFIX));
    assertFalse(startedMessage.contains(EXPECTED_ARGUMENT_1));

    assertFalse(completedMessage.contains(ARGUMENTS_PREFIX));
  }

  @Test
  void testLogExecution_shouldLogResult_whenResultLoggingIsEnabled() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(true);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertFalse(startedMessage.contains(RESULT_PREFIX));
    assertTrue(completedMessage.contains(RESULT_PREFIX + EXPECTED_RESULT));
  }

  @Test
  void testLogExecution_shouldLogAtInfoLevel_whenInfoLevelIsConfigured() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(INFO);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(Level.INFO, logAppender.list.getFirst().getLevel());
    assertEquals(Level.INFO, logAppender.list.getLast().getLevel());
  }

  @Test
  void testLogExecution_shouldLogAtDebugLevel_whenDebugLevelIsConfigured() throws Throwable {
    // Given
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(Level.DEBUG, logAppender.list.getFirst().getLevel());
    assertEquals(Level.DEBUG, logAppender.list.getLast().getLevel());
  }

  private void mockArguments(String sampleMethodName) throws Throwable {
    Object[] arguments = {ARGUMENT_1, ARGUMENT_2, ARGUMENT_3};

    when(joinPoint.getArgs()).thenReturn(arguments);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(signature.getMethod()).thenReturn(getSampleMethod(sampleMethodName));
  }

  private Method getSampleMethod(String name) throws NoSuchMethodException {
    return LoggingAspectUnitTests.class.getDeclaredMethod(
        name, Long.class, String.class, Boolean.class);
  }

  private void sampleMethod(Long first, String second, Boolean third) {}

  private void redactedSampleMethod(Long first, @Redacted String second, Boolean third) {}

  interface SampleInterface<T> {
    void process(String name, T value);
  }

  static class SampleImplementation implements SampleInterface<String> {
    @Override
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

  private String getLogMessage(int index) {
    List<ILoggingEvent> logs = logAppender.list;

    assertFalse(logs.isEmpty());

    return logs.get(index).getFormattedMessage();
  }

  private void assertStartedLog(ILoggingEvent log) {
    String message = log.getFormattedMessage();

    assertTrue(message.contains(STARTED));
    assertFalse(message.contains(RESULT_PREFIX));
  }

  private void assertCompletedLog(ILoggingEvent log) {
    String message = log.getFormattedMessage();

    assertTrue(message.contains(COMPLETED_PREFIX));
    assertTrue(message.contains(MILLISECONDS_SUFFIX));
  }
}
