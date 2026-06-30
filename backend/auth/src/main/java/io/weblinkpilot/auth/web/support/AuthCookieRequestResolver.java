package io.weblinkpilot.auth.web.support;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.shared.api.auth.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieRequestResolver {

  private final AuthCookieService authCookieService;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring singleton dependency injection stores shared service collaborators.")
  public AuthCookieRequestResolver(AuthCookieService authCookieService) {
    this.authCookieService = authCookieService;
  }

  public String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
    if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
      return request.refreshToken();
    }
    return resolveCookie(
        httpRequest, authCookieService.getCookieName(), "Refresh token is required");
  }

  public String resolveGithubState(HttpServletRequest request) {
    return resolveCookie(
        request, authCookieService.getGithubStateCookieName(), "OAuth state is required");
  }

  private String resolveCookie(HttpServletRequest request, String name, String missingMessage) {
    if (request.getCookies() == null) {
      throw new IllegalArgumentException(missingMessage);
    }
    for (var cookie : request.getCookies()) {
      if (cookie != null && cookie.getName().equals(name)) {
        String value = cookie.getValue();
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    throw new IllegalArgumentException(missingMessage);
  }
}
