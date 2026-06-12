package io.weblinkpilot.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.auth.exception.AccountActionRequestCooldownException;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthExceptionHandlerTest {

  private final AuthExceptionHandler handler = new AuthExceptionHandler();

  @Test
  void accountDisabledUsesGenericCredentialsResponse() {
    HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

    var response = handler.accountDisabled(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    assertThat(response.getBody().message()).isEqualTo("Incorrect username or password");
  }

  @Test
  void invalidCredentialsUsesGenericCredentialsResponse() {
    HttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

    var response = handler.invalidCredentials(new InvalidCredentialsException(), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    assertThat(response.getBody().message()).isEqualTo("Incorrect username or password");
  }

  @Test
  void requestCooldownReturnsTooManyRequestsWithRetryAfter() {
    HttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/v1/auth/email-verification/request");

    var response =
        handler.requestCooldown(
            new AccountActionRequestCooldownException("verification email", 30), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("REQUEST_COOLDOWN");
    assertThat(response.getBody().message())
        .isEqualTo("Please wait 30 seconds before requesting another verification email.");
  }
}
