package io.weblinkpilot.auth.event;

public record PasswordResetLinkRequestedEvent(String email, String link) {}
