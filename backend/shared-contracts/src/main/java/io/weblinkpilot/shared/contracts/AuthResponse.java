package io.weblinkpilot.shared.contracts;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}
