package com.example.core.exception;

public abstract class BusinessException extends RuntimeException {

  protected BusinessException(String message) {
    super(message);
  }
}
