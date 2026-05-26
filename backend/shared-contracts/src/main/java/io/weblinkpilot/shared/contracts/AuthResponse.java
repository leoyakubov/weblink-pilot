package io.weblinkpilot.shared.contracts;

public record AuthResponse(String token, String refreshToken, String username, String role) {}
