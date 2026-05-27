package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(@NotBlank String token) {}
