package io.weblinkpilot.url.service;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String code) {
        super("Short link not found: " + code);
    }
}
