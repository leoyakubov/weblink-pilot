package io.weblinkpilot.shared.api.auth;

public record AuthResponse(String token, String username, String role) {}
