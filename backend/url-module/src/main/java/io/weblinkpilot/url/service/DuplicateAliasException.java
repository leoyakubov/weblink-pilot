package io.weblinkpilot.url.service;

public class DuplicateAliasException extends RuntimeException {
    public DuplicateAliasException(String alias) {
        super("Custom alias already exists: " + alias);
    }
}
