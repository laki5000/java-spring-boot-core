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

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private Signature signature;

  @Mock private LogExecution logExecution;

  private LoggingAspect loggingAspect;

  @BeforeEach
  void setUp() {
    loggingAspect = new LoggingAspect();
  }

  @Test
  void testLogExecution_shouldReturnResultFromProceed() throws Throwable {
    // Given
    when(joinPoint.getTarget()).thenReturn(this);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn(METHOD_NAME);
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

    when(joinPoint.getTarget()).thenReturn(this);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn(METHOD_NAME);
    when(joinPoint.proceed()).thenThrow(expectedException);

    // When / Then
    RuntimeException actualException =
        assertThrows(
            RuntimeException.class, () -> loggingAspect.logExecution(joinPoint, logExecution));

    assertEquals(expectedException, actualException);
  }
}
