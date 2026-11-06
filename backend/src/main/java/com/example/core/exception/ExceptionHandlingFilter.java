package com.example.core.exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ExceptionHandlingFilter extends OncePerRequestFilter {

  private final HandlerExceptionResolver handlerExceptionResolver;
  private final LocaleResolver localeResolver;

  public ExceptionHandlingFilter(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
      LocaleResolver localeResolver) {
    this.handlerExceptionResolver = handlerExceptionResolver;
    this.localeResolver = localeResolver;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      filterChain.doFilter(request, response);
    } catch (Exception exception) {
      if (response.isCommitted()) {
        throw exception;
      }

      response.resetBuffer();
      response.setHeader(HttpHeaders.CONTENT_TYPE, null);
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, null);
      LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));

      try {
        if (handlerExceptionResolver.resolveException(request, response, null, exception) == null) {
          throw exception;
        }
      } finally {
        LocaleContextHolder.resetLocaleContext();
      }
    }
  }
}
