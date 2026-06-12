package io.weblinkpilot.auth.exception;

public class AccountActionRequestCooldownException extends RuntimeException {

  private final long retryAfterSeconds;

  public AccountActionRequestCooldownException(String action, long retryAfterSeconds) {
    super(buildMessage(action, retryAfterSeconds));
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  private static String buildMessage(String action, long retryAfterSeconds) {
    String label = action == null || action.isBlank() ? "request" : action;
    return "Please wait " + retryAfterSeconds + " seconds before requesting another " + label + ".";
  }
}
