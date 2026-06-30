package io.weblinkpilot.shared.api.analytics;

import java.util.List;

public record AnalyticsDetailsResponse(
    String code,
    List<AnalyticsBucketStatResponse> timelineByDay,
    List<AnalyticsBucketStatResponse> timelineByHour,
    List<AnalyticsBreakdownStatResponse> browserBreakdown,
    List<AnalyticsBreakdownStatResponse> deviceBreakdown,
    List<AnalyticsBreakdownStatResponse> referrerBreakdown,
    List<AnalyticsEventResponse> recentEvents,
    List<AnalyticsBucketStatResponse> sourceTrendByDay,
    List<AnalyticsBucketStatResponse> visitorTrendByDay) {

  public AnalyticsDetailsResponse {
    timelineByDay = timelineByDay == null ? List.of() : List.copyOf(timelineByDay);
    timelineByHour = timelineByHour == null ? List.of() : List.copyOf(timelineByHour);
    browserBreakdown = browserBreakdown == null ? List.of() : List.copyOf(browserBreakdown);
    deviceBreakdown = deviceBreakdown == null ? List.of() : List.copyOf(deviceBreakdown);
    referrerBreakdown = referrerBreakdown == null ? List.of() : List.copyOf(referrerBreakdown);
    recentEvents = recentEvents == null ? List.of() : List.copyOf(recentEvents);
    sourceTrendByDay = sourceTrendByDay == null ? List.of() : List.copyOf(sourceTrendByDay);
    visitorTrendByDay = visitorTrendByDay == null ? List.of() : List.copyOf(visitorTrendByDay);
  }
}
