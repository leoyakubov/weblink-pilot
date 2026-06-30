package io.weblinkpilot.shared.api.auth;

import java.util.List;

public record AccountProfileResponse(
    String username,
    String role,
    String email,
    boolean emailVerified,
    String createdAt,
    String lastLoginAt,
    List<AccountIdentityResponse> socialIdentities) {

  public AccountProfileResponse {
    socialIdentities = socialIdentities == null ? List.of() : List.copyOf(socialIdentities);
  }
}
