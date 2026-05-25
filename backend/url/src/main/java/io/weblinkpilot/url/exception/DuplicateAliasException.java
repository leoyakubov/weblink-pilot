package io.weblinkpilot.url.exception;

public class DuplicateAliasException extends RuntimeException {
  public DuplicateAliasException(String alias) {
    super("Custom alias already exists: " + alias);
  }
}
