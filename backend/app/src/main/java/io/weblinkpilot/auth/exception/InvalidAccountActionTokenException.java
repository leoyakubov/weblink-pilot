package io.weblinkpilot.auth.exception;

public class InvalidAccountActionTokenException extends RuntimeException {

  public InvalidAccountActionTokenException() {
    super("Invalid or expired account action token");
  }
}
