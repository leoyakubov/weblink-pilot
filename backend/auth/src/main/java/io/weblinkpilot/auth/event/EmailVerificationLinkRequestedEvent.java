package io.weblinkpilot.auth.event;

public record EmailVerificationLinkRequestedEvent(String email, String link) {}
