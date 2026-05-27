package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(@NotBlank @Email String email) {}
