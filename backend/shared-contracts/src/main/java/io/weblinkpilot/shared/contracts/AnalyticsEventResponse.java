package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record AnalyticsEventResponse(
    OffsetDateTime clickedAt,
    LinkTrackingSource eventSource,
    String referrer,
    String country,
    String browserFamily,
    String deviceType) {}
