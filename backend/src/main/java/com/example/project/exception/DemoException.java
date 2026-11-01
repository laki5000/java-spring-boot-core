package com.example.project.exception;

import com.example.core.exception.BusinessException;

public class DemoException extends BusinessException {

  public DemoException(String message) {
    super(message);
  }
}
