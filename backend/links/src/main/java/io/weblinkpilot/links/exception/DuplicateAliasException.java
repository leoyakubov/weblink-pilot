package io.weblinkpilot.links.exception;

public class DuplicateAliasException extends RuntimeException {
  public DuplicateAliasException(String alias) {
    super("Custom alias already exists: " + alias);
  }
}
