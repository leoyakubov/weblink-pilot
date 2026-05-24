package io.weblinkpilot.auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("That username is already taken: " + username);
    }
}
