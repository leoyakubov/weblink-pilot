package io.weblinkpilot.auth.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String username) {
        super("This account is disabled: " + username);
    }
}
