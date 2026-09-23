package com.example.core.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
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

  private static final String ARGUMENT_NAME_1 = "first";
  private static final String ARGUMENT_NAME_2 = "second";
  private static final String ARGUMENT_NAME_3 = "third";

  private static final String EXPECTED_ARGUMENT_1 = "0=first: 1";
  private static final String EXPECTED_ARGUMENT_2 = "1=second: test";
  private static final String EXPECTED_ARGUMENT_3 = "2=third: true";
  private static final String REDACTED_ARGUMENT_2 = "1=second: [REDACTED]";

  private static final String STARTED = "LoggingAspectUnitTests.testMethod started";
  private static final String COMPLETED_PREFIX = "LoggingAspectUnitTests.testMethod completed in ";
  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = "arguments=";
  private static final String RESULT_PREFIX = "result=";

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private CodeSignature signature;

  @Mock private LogExecution logExecution;

  private LoggingAspect loggingAspect;

  private Logger logger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    loggingAspect = new LoggingAspect();

    logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
    logger.setLevel(Level.DEBUG);

    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);

    when(joinPoint.getTarget()).thenReturn(this);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn(METHOD_NAME);
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
  void testLogExecution_shouldPropagateExceptionFromProceed() throws Throwable {
    // Given
    RuntimeException expectedException = new RuntimeException(EXCEPTION_MESSAGE);

    when(joinPoint.proceed()).thenThrow(expectedException);
    when(logExecution.level()).thenReturn(DEBUG);

    // When / Then
    RuntimeException actualException =
        assertThrows(
            RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

    assertEquals(expectedException, actualException);
    verify(joinPoint).proceed();

    assertEquals(1, logAppender.list.size());
    assertStartedLog(logAppender.list.getFirst());
  }

  @Test
  void testLogExecution_shouldLogAllArguments_whenArgumentLoggingIsEnabledAndIndexesAreEmpty()
      throws Throwable {
    // Given
    mockArguments();

    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.argumentIndexes()).thenReturn(new int[0]);
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
  void testLogExecution_shouldLogSelectedArgumentsAndRedactOthers_whenArgumentIndexesAreProvided()
      throws Throwable {
    // Given
    mockArguments();

    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.argumentIndexes()).thenReturn(new int[] {0, 2});
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
  void testLogExecution_shouldIgnoreArgumentIndexes_whenArgumentLoggingIsDisabled()
      throws Throwable {
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
  void testLogExecution_shouldIgnoreInvalidArgumentIndexes_whenArgumentIndexesAreProvided()
      throws Throwable {
    // Given
    mockArguments();

    when(logExecution.level()).thenReturn(DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.argumentIndexes()).thenReturn(new int[] {-1, 0, 99, 2});
    when(logExecution.logResult()).thenReturn(false);

    // When
    loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    String message = getLogMessage(0);

    assertTrue(message.contains(EXPECTED_ARGUMENT_1));
    assertTrue(message.contains(EXPECTED_ARGUMENT_3));
    assertTrue(message.contains(REDACTED_ARGUMENT_2));
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

  private void mockArguments() throws Throwable {
    Object[] arguments = {ARGUMENT_1, ARGUMENT_2, ARGUMENT_3};

    when(joinPoint.getArgs()).thenReturn(arguments);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(signature.getParameterNames())
        .thenReturn(new String[] {ARGUMENT_NAME_1, ARGUMENT_NAME_2, ARGUMENT_NAME_3});
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
