package com.example.core.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

  private static final Long ARGUMENT_1 = 1L;
  private static final String ARGUMENT_2 = "test";
  private static final Boolean ARGUMENT_3 = true;

  private static final String EXPECTED_ARGUMENT_1 = "0=first: 1";
  private static final String EXPECTED_ARGUMENT_2 = "1=second: test";
  private static final String EXPECTED_ARGUMENT_3 = "2=third: true";

  private static final String CARRIAGE_RETURN = "\r";
  private static final String LINE_FEED = "\n";
  private static final String MULTILINE_VALUE = "Bob\r\nINFO fake log line";
  private static final String ESCAPED_MULTILINE_VALUE = "Bob\\r\\nINFO fake log line";

  private static final String MILLISECONDS_SUFFIX = " ms";
  private static final String ARGUMENTS_PREFIX = "arguments=";
  private static final String RESULT_PREFIX = "result=";

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
  void logExecution_shouldReturnResultAndLogStartedAndCompleted() {
    // When
    Object result = sampleService.plain();

    // Then
    assertThat(result).isEqualTo(EXPECTED_RESULT);
    assertThat(target.invocations).isEqualTo(1);

    assertThat(logAppender.list).hasSize(2);
    assertStartedLog(logAppender.list.getFirst(), SampleService.PLAIN);
    assertCompletedLog(logAppender.list.getLast(), SampleService.PLAIN);
  }

  @Test
  void logExecution_shouldNotLog_whenMethodIsNotAnnotated() {
    // When
    Object result = sampleService.notAnnotated();

    // Then
    assertThat(result).isEqualTo(EXPECTED_RESULT);
    assertThat(logAppender.list).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"DEBUG, DEBUG, 2", "DEBUG, INFO, 0", "INFO, INFO, 2", "INFO, WARN, 0"})
  void logExecution_shouldLogOnlyWhenAnnotationLevelIsEnabled(
      LogLevel annotationLevel, String loggerLevel, int expectedLogCount) {
    // Given
    Level level = Level.toLevel(loggerLevel, null);
    assertThat(level).isNotNull();
    logger.setLevel(level);

    // When
    Object result = annotationLevel == LogLevel.INFO ? sampleService.info() : sampleService.plain();

    // Then
    assertThat(result).isEqualTo(EXPECTED_RESULT);
    assertThat(logAppender.list).hasSize(expectedLogCount);
    logAppender.list.forEach(
        event -> assertThat(event.getLevel()).isEqualTo(Level.toLevel(annotationLevel.name())));
  }

  @Test
  void logExecution_shouldNotFormatArgumentsAndResult_whenLogLevelIsDisabled() {
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
    assertThat(result).isSameAs(countingObject);
    assertThat(target.invocations).isEqualTo(1);
    assertThat(logAppender.list).isEmpty();
    assertThat(toStringCalls.get()).isEqualTo(0);
  }

  @Test
  void logExecution_shouldLogWithTargetClassLogger() {
    // When
    sampleService.plain();

    // Then
    assertThat(logAppender.list.getFirst().getLoggerName())
        .isEqualTo(SampleService.class.getName());
  }

  @Test
  void logExecution_shouldLogFailedAndPropagateException_whenMethodThrows() {
    // Given
    RuntimeException expectedException = new RuntimeException("test");

    // When
    RuntimeException actualException =
        catchThrowableOfType(RuntimeException.class, () -> sampleService.fail(expectedException));

    // Then
    assertThat(actualException).isSameAs(expectedException);

    assertThat(logAppender.list).hasSize(2);
    assertStartedLog(logAppender.list.getFirst(), SampleService.FAIL);

    String failedMessage = getLogMessage(1);

    assertThat(failedMessage).startsWith(SampleService.FAIL + " failed after ");
    assertThat(failedMessage).contains(MILLISECONDS_SUFFIX);
    assertThat(failedMessage).contains("exception=" + expectedException);
    assertThat(logAppender.list.get(1).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(logAppender.list.get(1).getThrowableProxy()).isNull();
  }

  @Test
  void logExecution_shouldLogAllArguments_whenArgumentLoggingIsEnabled() {
    // When
    sampleService.withArguments(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertThat(startedMessage).contains(ARGUMENTS_PREFIX);
    assertThat(startedMessage).contains(EXPECTED_ARGUMENT_1);
    assertThat(startedMessage).contains(EXPECTED_ARGUMENT_2);
    assertThat(startedMessage).contains(EXPECTED_ARGUMENT_3);

    assertThat(completedMessage).doesNotContain(ARGUMENTS_PREFIX);
    assertThat(completedMessage).doesNotContain(RESULT_PREFIX);
  }

  @Test
  void logExecution_shouldRedactArgument_whenParameterIsAnnotatedWithRedacted() {
    // When
    sampleService.withRedactedArgument(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3);

    // Then
    String message = getLogMessage(0);

    assertThat(message).contains(EXPECTED_ARGUMENT_1);
    assertThat(message).contains(EXPECTED_ARGUMENT_3);
    assertThat(message).contains("1=second: [REDACTED]");

    assertThat(message).doesNotContain(EXPECTED_ARGUMENT_2);
  }

  @Test
  void logExecution_shouldRedactArgument_whenAnnotationIsOnImplementationOfInterfaceMethod() {
    // Given
    SampleInterface<String> sampleInterface = createProxy(new SampleImplementation());
    assertThat(AopUtils.isJdkDynamicProxy(sampleInterface)).isTrue();

    // When
    sampleInterface.process(ARGUMENT_2, ARGUMENT_2);

    // Then
    String message = getLogMessage(0);

    assertThat(message).contains("1=value: [REDACTED]");
    assertThat(message).doesNotContain("1=value: test");
  }

  @Test
  void logExecution_shouldSanitizeArgumentsAndResult() {
    // When
    sampleService.echo(ARGUMENT_1, MULTILINE_VALUE, ARGUMENT_3);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertThat(startedMessage).contains("1=second: " + ESCAPED_MULTILINE_VALUE);
    assertThat(completedMessage).contains(RESULT_PREFIX + ESCAPED_MULTILINE_VALUE);
    assertThat(startedMessage).doesNotContain(LINE_FEED).doesNotContain(CARRIAGE_RETURN);
    assertThat(completedMessage).doesNotContain(LINE_FEED).doesNotContain(CARRIAGE_RETURN);
  }

  @Test
  void logExecution_shouldSanitizeException() {
    // When
    assertThatThrownBy(() -> sampleService.fail(new RuntimeException(MULTILINE_VALUE)))
        .isInstanceOf(RuntimeException.class);

    // Then
    String failedMessage = getLogMessage(1);

    assertThat(failedMessage).contains(ESCAPED_MULTILINE_VALUE);
    assertThat(failedMessage).doesNotContain(LINE_FEED).doesNotContain(CARRIAGE_RETURN);
  }

  @Test
  void logExecution_shouldNotLogArguments_whenArgumentLoggingIsDisabled() {
    // When
    sampleService.withoutArgumentLogging(ARGUMENT_1);

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertThat(startedMessage).doesNotContain(ARGUMENTS_PREFIX);
    assertThat(startedMessage).doesNotContain(EXPECTED_ARGUMENT_1);

    assertThat(completedMessage).doesNotContain(ARGUMENTS_PREFIX);
  }

  @Test
  void logExecution_shouldLogResult_whenResultLoggingIsEnabled() {
    // When
    sampleService.withResult();

    // Then
    String startedMessage = getLogMessage(0);
    String completedMessage = getLogMessage(1);

    assertThat(startedMessage).doesNotContain(RESULT_PREFIX);
    assertThat(completedMessage).contains(RESULT_PREFIX + EXPECTED_RESULT);
  }

  private static <T> T createProxy(Object target) {
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.addAspect(new LoggingAspect());

    return factory.getProxy();
  }

  private String getLogMessage(int index) {
    List<ILoggingEvent> logs = logAppender.list;

    assertThat(logs).isNotEmpty();

    return logs.get(index).getFormattedMessage();
  }

  private void assertStartedLog(ILoggingEvent log, String methodName) {
    String message = log.getFormattedMessage();

    assertThat(message).startsWith(methodName + " started");
    assertThat(message).doesNotContain(RESULT_PREFIX);
  }

  private void assertCompletedLog(ILoggingEvent log, String methodName) {
    String message = log.getFormattedMessage();

    assertThat(message).startsWith(methodName + " completed in ");
    assertThat(message).contains(MILLISECONDS_SUFFIX);
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
}
