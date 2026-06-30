package io.weblinkpilot.shared.api.analytics;

import io.weblinkpilot.shared.types.LinkTrackingSource;
import java.time.OffsetDateTime;

public record AnalyticsEventResponse(
    OffsetDateTime clickedAt,
    LinkTrackingSource eventSource,
    String referrer,
    String country,
    String browserFamily,
    String deviceType) {}
