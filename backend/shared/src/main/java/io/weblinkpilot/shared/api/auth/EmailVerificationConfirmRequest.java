package io.weblinkpilot.shared.api.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(@NotBlank String token) {}
