package io.weblinkpilot.auth.web.error;

import io.weblinkpilot.auth.exception.AccountActionRequestCooldownException;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.EmailAlreadyExistsException;
import io.weblinkpilot.auth.exception.EmailNotVerifiedException;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.exception.UsernameAlreadyExistsException;
import io.weblinkpilot.shared.api.common.ApiErrorResponse;
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

  private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
  private static final String BAD_REQUEST_CODE = "BAD_REQUEST";
  private static final String INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS";
  private static final String INVALID_REFRESH_TOKEN_CODE = "INVALID_REFRESH_TOKEN";
  private static final String USERNAME_EXISTS_CODE = "USERNAME_EXISTS";
  private static final String EMAIL_EXISTS_CODE = "EMAIL_EXISTS";
  private static final String EMAIL_NOT_VERIFIED_CODE = "EMAIL_NOT_VERIFIED";
  private static final String INVALID_ACCOUNT_ACTION_TOKEN_CODE = "INVALID_ACCOUNT_ACTION_TOKEN";
  private static final String REQUEST_COOLDOWN_CODE = "REQUEST_COOLDOWN";
  private static final String RETRY_AFTER_HEADER = "Retry-After";
  private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
  private static final String GENERIC_INVALID_CREDENTIALS_MESSAGE =
      "Incorrect username or password";
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
                        .orElse(VALIDATION_FAILED_MESSAGE));
    return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_CODE, message, request.getRequestURI());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> badRequest(
      IllegalArgumentException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, BAD_REQUEST_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> invalidCredentials(
      InvalidCredentialsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        INVALID_CREDENTIALS_CODE,
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ApiErrorResponse> invalidRefreshToken(
      InvalidRefreshTokenException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        INVALID_REFRESH_TOKEN_CODE,
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(UsernameAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> usernameExists(
      UsernameAlreadyExistsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, USERNAME_EXISTS_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> emailExists(
      EmailAlreadyExistsException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, EMAIL_EXISTS_CODE, exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<ApiErrorResponse> emailNotVerified(
      EmailNotVerifiedException exception, HttpServletRequest request) {
    return build(
        HttpStatus.FORBIDDEN,
        EMAIL_NOT_VERIFIED_CODE,
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(InvalidAccountActionTokenException.class)
  public ResponseEntity<ApiErrorResponse> invalidAccountActionToken(
      InvalidAccountActionTokenException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        INVALID_ACCOUNT_ACTION_TOKEN_CODE,
        exception.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ApiErrorResponse> accountDisabled(HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        INVALID_CREDENTIALS_CODE,
        GENERIC_INVALID_CREDENTIALS_MESSAGE,
        request.getRequestURI());
  }

  @ExceptionHandler(AccountActionRequestCooldownException.class)
  public ResponseEntity<ApiErrorResponse> requestCooldown(
      AccountActionRequestCooldownException exception, HttpServletRequest request) {
    return build(
        HttpStatus.TOO_MANY_REQUESTS,
        REQUEST_COOLDOWN_CODE,
        exception.getMessage(),
        request.getRequestURI(),
        exception.getRetryAfterSeconds());
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, String code, String message, String path) {
    return build(status, code, message, path, null);
  }

  private ResponseEntity<ApiErrorResponse> build(
      HttpStatus status, String code, String message, String path, Long retryAfterSeconds) {
    log.warn(
        "request.rejected status={} code={} path={} message={}",
        status.value(),
        code,
        path,
        message);
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
    if (retryAfterSeconds != null && retryAfterSeconds > 0) {
      builder.header(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
    }
    return builder.body(AuthErrorResponseFactory.create(status, code, message, path));
  }
}
