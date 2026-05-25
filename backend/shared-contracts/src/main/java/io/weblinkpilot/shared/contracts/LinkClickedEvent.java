package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record LinkClickedEvent(
    String code,
    OffsetDateTime clickedAt,
    LinkTrackingSource source,
    String ipAddress,
    String userAgent,
    String referrer,
    String country) {}
