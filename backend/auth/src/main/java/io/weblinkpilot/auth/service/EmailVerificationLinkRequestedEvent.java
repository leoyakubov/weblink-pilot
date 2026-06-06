package io.weblinkpilot.auth.service;

public record EmailVerificationLinkRequestedEvent(String email, String link) {}
