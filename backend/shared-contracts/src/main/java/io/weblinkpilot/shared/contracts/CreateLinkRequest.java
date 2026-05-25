package io.weblinkpilot.shared.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public record CreateLinkRequest(
    @NotBlank(message = "Original URL is required") String originalUrl,
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{3,64}$",
            message =
                "Custom alias must be 3-64 characters long and contain only letters, digits, dash or underscore")
        String customAlias,
    OffsetDateTime expiresAt) {}
