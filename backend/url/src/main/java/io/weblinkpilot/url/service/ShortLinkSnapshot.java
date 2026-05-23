package io.weblinkpilot.url.service;

import java.time.OffsetDateTime;

public record ShortLinkSnapshot(
        String code,
        String originalUrl,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt
) {
}
