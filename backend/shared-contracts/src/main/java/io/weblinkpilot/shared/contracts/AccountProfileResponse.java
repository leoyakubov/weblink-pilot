package io.weblinkpilot.shared.contracts;

import java.util.List;

public record AccountProfileResponse(
    String username,
    String role,
    String email,
    boolean emailVerified,
    String createdAt,
    String lastLoginAt,
    List<AccountIdentityResponse> socialIdentities) {}
