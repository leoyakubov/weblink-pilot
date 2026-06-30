package io.weblinkpilot.shared.api.analytics;

public record AnalyticsBucketStatResponse(
    String bucket, long totalClicks, long redirectClicks, long qrScans, long uniqueVisitors) {}
