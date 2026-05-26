package io.weblinkpilot.auth.web;

import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.EmailAlreadyExistsException;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.exception.UsernameAlreadyExistsException;
import io.weblinkpilot.shared.contracts.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> validation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElseGet(
                () ->
                    exception.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage())
                        .orElse("Validation failed"));
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> badRequest(
      IllegalArgumentException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> invalidCredentials(
      InvalidCredentialsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        "INVALID_CREDENTIALS",
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ApiErrorResponse> invalidRefreshToken(
      InvalidRefreshTokenException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        "INVALID_REFRESH_TOKEN",
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(UsernameAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> usernameExists(
      UsernameAlreadyExistsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, "USERNAME_EXISTS", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> emailExists(
      EmailAlreadyExistsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, "EMAIL_EXISTS", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidAccountActionTokenException.class)
  public ResponseEntity<ApiErrorResponse> invalidAccountActionToken(
      InvalidAccountActionTokenException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        "INVALID_ACCOUNT_ACTION_TOKEN",
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ApiErrorResponse> accountDisabled(HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        "INVALID_CREDENTIALS",
        "Incorrect username or password",
        request.getRequestURI());
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
        .body(AuthErrorResponseFactory.create(status, code, message, path));
  }
}
