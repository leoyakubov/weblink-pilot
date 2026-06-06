package io.weblinkpilot.auth.service;

public record PasswordResetLinkRequestedEvent(String email, String link) {}
