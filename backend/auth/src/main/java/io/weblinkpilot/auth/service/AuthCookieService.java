package io.weblinkpilot.auth.service;

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
  private final String githubStateCookieName;
  private final String githubStateCookiePath;
  private final Duration githubStateCookieMaxAge;

  public AuthCookieService(AuthProperties authProperties) {
    this.cookieName = authProperties.getRefreshCookieName();
    this.cookiePath = authProperties.getRefreshCookiePath();
    this.sameSite = authProperties.getRefreshCookieSameSite();
    this.secure = authProperties.isRefreshCookieSecure();
    this.maxAge = Duration.ofDays(authProperties.getRefreshTokenTtlDays());
    this.githubStateCookieName = authProperties.getGithubStateCookieName();
    this.githubStateCookiePath = authProperties.getGithubStateCookiePath();
    this.githubStateCookieMaxAge =
        Duration.ofMinutes(authProperties.getGithubLoginTicketTtlMinutes());
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

  public String getGithubStateCookieName() {
    return githubStateCookieName;
  }

  public ResponseCookie createGithubStateCookie(String state) {
    return ResponseCookie.from(githubStateCookieName, state)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(githubStateCookiePath)
        .maxAge(githubStateCookieMaxAge)
        .build();
  }

  public ResponseCookie clearGithubStateCookie() {
    return ResponseCookie.from(githubStateCookieName, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(githubStateCookiePath)
        .maxAge(Duration.ZERO)
        .build();
  }

  private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
    return ResponseCookie.from(cookieName, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(cookiePath);
  }
}
