package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record LinkClickedEvent(
        String code,
        OffsetDateTime clickedAt,
        String ipAddress,
        String userAgent,
        String referrer
) {
}
