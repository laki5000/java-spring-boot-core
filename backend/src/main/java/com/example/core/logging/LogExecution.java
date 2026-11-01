package com.example.core.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {

  LogLevel level() default LogLevel.DEBUG;

  boolean logArguments() default false;

  int[] argumentIndexes() default {};

  boolean logResult() default false;
}
