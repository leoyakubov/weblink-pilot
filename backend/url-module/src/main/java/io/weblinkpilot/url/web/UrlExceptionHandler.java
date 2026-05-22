package io.weblinkpilot.url.web;

import io.weblinkpilot.shared.contracts.ApiErrorResponse;
import io.weblinkpilot.url.exception.DuplicateAliasException;
import io.weblinkpilot.url.exception.UrlExpiredException;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import java.time.OffsetDateTime;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateAliasException.class)
    public ResponseEntity<ApiErrorResponse> conflict(DuplicateAliasException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(UrlNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ApiErrorResponse> gone(UrlExpiredException exception, HttpServletRequest request) {
        return build(HttpStatus.GONE, "LINK_EXPIRED", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message, String path) {
        log.warn("request.rejected status={} code={} path={} message={}", status.value(), code, path, message);
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path
        ));
    }
}
