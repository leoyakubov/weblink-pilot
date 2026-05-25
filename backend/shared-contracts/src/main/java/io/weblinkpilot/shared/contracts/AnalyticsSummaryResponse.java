package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;
import java.util.List;

public record AnalyticsSummaryResponse(
    String code,
    long totalClicks,
    long redirectClicks,
    long qrScans,
    long uniqueVisitors,
    OffsetDateTime lastClickedAt,
    String lastReferrer,
    String lastBrowserFamily,
    String lastDeviceType,
    List<AnalyticsCountryStatResponse> topCountries) {

  public AnalyticsSummaryResponse {
    topCountries = topCountries == null ? List.of() : List.copyOf(topCountries);
  }
}
