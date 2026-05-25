package io.weblinkpilot.url.exception;

public class UrlExpiredException extends RuntimeException {
  public UrlExpiredException(String code) {
    super("Short link is expired: " + code);
  }
}
