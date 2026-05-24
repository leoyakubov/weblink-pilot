package io.weblinkpilot.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String username) {
        super("Account is disabled: " + username);
    }
}
