package io.weblinkpilot.url.service;

import java.time.OffsetDateTime;

public record ShortLinkSnapshot(
    String code,
    String originalUrl,
    String ownerUsername,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    long clickCount) {}
