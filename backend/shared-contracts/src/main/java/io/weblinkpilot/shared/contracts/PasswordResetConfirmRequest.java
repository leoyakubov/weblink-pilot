package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetConfirmRequest(
    @NotBlank(message = "Reset token is required") String token,
    @NotBlank(message = "Password is required") String password) {}
