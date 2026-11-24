package com.example.core.exception;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.ObjectUtils;
import org.springframework.web.ErrorResponseException;

public abstract class ApiException extends ErrorResponseException {

  protected ApiException(HttpStatus status, String messageKey, Object... arguments) {
    super(
        status, ProblemDetail.forStatusAndDetail(status, messageKey), null, messageKey, arguments);
  }

  @Override
  public String getMessage() {
    Object[] arguments = getDetailMessageArguments();

    return ObjectUtils.isEmpty(arguments)
        ? getDetailMessageCode()
        : getDetailMessageCode() + " " + Arrays.toString(arguments);
  }
}
