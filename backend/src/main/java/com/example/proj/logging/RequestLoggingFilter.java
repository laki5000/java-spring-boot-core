package com.example.proj.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final String REQUEST_LOG_FORMAT = "{} {} -> {} ({} ms)";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    long start = System.nanoTime();
    int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    try {
      filterChain.doFilter(request, response);
      status = response.getStatus();
    } finally {
      long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      log.info(REQUEST_LOG_FORMAT, request.getMethod(), request.getRequestURI(), status, duration);
    }
  }
}
