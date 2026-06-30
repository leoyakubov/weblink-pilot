package io.weblinkpilot.auth.web.error;

import io.weblinkpilot.shared.api.common.ApiErrorResponse;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

public final class AuthErrorResponseFactory {

  private AuthErrorResponseFactory() {}

  public static ApiErrorResponse create(
      HttpStatus status, String code, String message, String path) {
    return new ApiErrorResponse(
        OffsetDateTime.now(), status.value(), status.getReasonPhrase(), code, message, path);
  }
}
