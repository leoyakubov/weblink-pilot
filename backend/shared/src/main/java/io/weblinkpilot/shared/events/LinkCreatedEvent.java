package io.weblinkpilot.shared.events;

import java.time.OffsetDateTime;

public record LinkCreatedEvent(
    String code,
    String originalUrl,
    String customAlias,
    String ownerUsername,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt) {}
