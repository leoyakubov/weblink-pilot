package io.weblinkpilot.shared.events;

import io.weblinkpilot.shared.types.LinkTrackingSource;
import java.time.OffsetDateTime;

public record LinkClickedEvent(
    String code,
    OffsetDateTime clickedAt,
    LinkTrackingSource source,
    String ipAddress,
    String userAgent,
    String referrer,
    String country) {}
