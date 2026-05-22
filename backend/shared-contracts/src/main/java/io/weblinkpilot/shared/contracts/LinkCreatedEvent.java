package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record LinkCreatedEvent(
        String code,
        String originalUrl,
        String customAlias,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
}
