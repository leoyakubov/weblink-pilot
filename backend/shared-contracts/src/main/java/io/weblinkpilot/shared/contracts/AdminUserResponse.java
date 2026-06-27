package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record AdminUserResponse(
    String username,
    String email,
    String role,
    boolean enabled,
    boolean emailVerified,
    OffsetDateTime createdAt,
    OffsetDateTime lastLoginAt) {}
