package com.example.core.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.slf4j.event.Level;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {

  Level level() default Level.DEBUG;

  boolean logArguments() default false;

  int[] argumentIndexes() default {};

  boolean logResult() default false;
}
