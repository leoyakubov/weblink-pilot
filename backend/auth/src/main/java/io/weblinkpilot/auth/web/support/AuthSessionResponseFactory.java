package io.weblinkpilot.auth.web.support;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.session.AuthSession;
import io.weblinkpilot.shared.api.auth.AuthResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionResponseFactory {

  private final AuthCookieService authCookieService;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring singleton dependency injection stores shared service collaborators.")
  public AuthSessionResponseFactory(AuthCookieService authCookieService) {
    this.authCookieService = authCookieService;
  }

  public ResponseEntity<AuthResponse> authenticated(AuthSession session) {
    return authenticated(
        session.token(), session.refreshToken(), session.username(), session.role());
  }

  public ResponseEntity<Void> loggedOut() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString())
        .build();
  }

  private ResponseEntity<AuthResponse> authenticated(
      String token, String refreshToken, String username, String role) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            authCookieService.createRefreshTokenCookie(refreshToken).toString())
        .body(new AuthResponse(token, username, role));
  }
}
