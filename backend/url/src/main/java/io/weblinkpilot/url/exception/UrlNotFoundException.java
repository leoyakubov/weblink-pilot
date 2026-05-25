package io.weblinkpilot.url.exception;

public class UrlNotFoundException extends RuntimeException {
  public UrlNotFoundException(String code) {
    super("Short link not found: " + code);
  }
}
