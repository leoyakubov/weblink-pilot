package io.weblinkpilot.auth.session;

public record AuthSession(String token, String refreshToken, String username, String role) {}
