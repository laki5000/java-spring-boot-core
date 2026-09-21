package com.example.core.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

@ExtendWith(MockitoExtension.class)
class LoggingAspectUnitTests {

  private static final String METHOD_NAME = "testMethod";
  private static final String EXPECTED_RESULT = "result";
  private static final String EXCEPTION_MESSAGE = "test";

  private static final Long ARGUMENT_1 = 1L;
  private static final String ARGUMENT_2 = "test";
  private static final Boolean ARGUMENT_3 = true;

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private Signature signature;

  @Mock private LogExecution logExecution;

  private LoggingAspect loggingAspect;

  @BeforeEach
  void setUp() {
    loggingAspect = new LoggingAspect();

    when(joinPoint.getTarget()).thenReturn(this);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn(METHOD_NAME);
  }

  @Test
  void testLogExecution_shouldReturnResultFromProceed() throws Throwable {
    // Given
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(Level.DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();
  }

  @Test
  void testLogExecution_shouldPropagateExceptionFromProceed() throws Throwable {
    // Given
    RuntimeException expectedException = new RuntimeException(EXCEPTION_MESSAGE);

    when(joinPoint.proceed()).thenThrow(expectedException);

    // When / Then
    RuntimeException actualException =
        assertThrows(
            RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

    assertEquals(expectedException, actualException);
    verify(joinPoint).proceed();
  }

  @Test
  void testLogExecution_shouldLogAllArguments_whenArgumentLoggingIsEnabledAndIndexesAreEmpty()
      throws Throwable {
    // Given
    Object[] arguments = {ARGUMENT_1, ARGUMENT_2, ARGUMENT_3};

    when(joinPoint.getArgs()).thenReturn(arguments);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(Level.DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.argumentIndexes()).thenReturn(new int[0]);
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();
  }

  @Test
  void
      testLogExecution_shouldLogSelectedArguments_whenArgumentLoggingIsEnabledAndIndexesAreProvided()
          throws Throwable {
    // Given
    Object[] arguments = {ARGUMENT_1, ARGUMENT_2, ARGUMENT_3};

    when(joinPoint.getArgs()).thenReturn(arguments);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(Level.DEBUG);
    when(logExecution.logArguments()).thenReturn(true);
    when(logExecution.argumentIndexes()).thenReturn(new int[] {0, 2});
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();
  }

  @Test
  void testLogExecution_shouldIgnoreArgumentIndexes_whenArgumentLoggingIsDisabled()
      throws Throwable {
    // Given
    Object[] arguments = {ARGUMENT_1, ARGUMENT_2, ARGUMENT_3};

    when(joinPoint.getArgs()).thenReturn(arguments);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(Level.DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(false);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();
  }

  @Test
  void testLogExecution_shouldLogResult_whenResultLoggingIsEnabled() throws Throwable {
    // Given
    when(joinPoint.getArgs()).thenReturn(new Object[0]);
    when(joinPoint.proceed()).thenReturn(EXPECTED_RESULT);
    when(logExecution.level()).thenReturn(Level.DEBUG);
    when(logExecution.logArguments()).thenReturn(false);
    when(logExecution.logResult()).thenReturn(true);

    // When
    Object result = loggingAspect.logExecution(joinPoint, logExecution);

    // Then
    assertEquals(EXPECTED_RESULT, result);
    verify(joinPoint).proceed();
  }
}
