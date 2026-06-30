package io.weblinkpilot.links.web.error;

import io.weblinkpilot.links.exception.DuplicateAliasException;
import io.weblinkpilot.links.exception.UrlExpiredException;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.shared.api.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UrlExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(UrlExceptionHandler.class);
  private static final String BAD_REQUEST_CODE = "BAD_REQUEST";
  private static final String CONFLICT_CODE = "CONFLICT";
  private static final String NOT_FOUND_CODE = "NOT_FOUND";
  private static final String LINK_EXPIRED_CODE = "LINK_EXPIRED";
  private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
  private static final String DEFAULT_VALIDATION_MESSAGE = "Validation failed";

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> badRequest(
      IllegalArgumentException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, BAD_REQUEST_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(DuplicateAliasException.class)
  public ResponseEntity<ApiErrorResponse> conflict(
      DuplicateAliasException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, CONFLICT_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(UrlNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> notFound(
      UrlNotFoundException exception, HttpServletRequest request) {
    return build(
        HttpStatus.NOT_FOUND, NOT_FOUND_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(UrlExpiredException.class)
  public ResponseEntity<ApiErrorResponse> gone(
      UrlExpiredException exception, HttpServletRequest request) {
    return build(
        HttpStatus.GONE, LINK_EXPIRED_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> validation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse(DEFAULT_VALIDATION_MESSAGE);
    return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, message, request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, String code, String message, String path) {
    log.warn(
        "request.rejected status={} code={} path={} message={}",
        status.value(),
        code,
        path,
        message);
    return ResponseEntity.status(status)
        .body(ApiErrorResponseFactory.create(status, code, message, path));
  }
}
