package io.weblinkpilot.auth.exception;

public class AccountDisabledException extends RuntimeException {

  public AccountDisabledException() {
    super("Incorrect username or password");
  }
}
