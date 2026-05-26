package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank(message = "Email is required") @Email String email) {}
