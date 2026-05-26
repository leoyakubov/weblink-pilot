package io.weblinkpilot.auth.web;

import io.weblinkpilot.auth.config.AuthProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

  private final String cookieName;
  private final String cookiePath;
  private final String sameSite;
  private final boolean secure;
  private final Duration maxAge;

  public AuthCookieService(AuthProperties authProperties) {
    this.cookieName = authProperties.getRefreshCookieName();
    this.cookiePath = authProperties.getRefreshCookiePath();
    this.sameSite = authProperties.getRefreshCookieSameSite();
    this.secure = authProperties.isRefreshCookieSecure();
    this.maxAge = Duration.ofDays(authProperties.getRefreshTokenTtlDays());
  }

  public String getCookieName() {
    return cookieName;
  }

  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return baseBuilder(refreshToken).maxAge(maxAge).build();
  }

  public ResponseCookie clearRefreshTokenCookie() {
    return baseBuilder("").maxAge(Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
    return ResponseCookie.from(cookieName, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(cookiePath);
  }
}
