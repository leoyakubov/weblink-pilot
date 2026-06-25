package io.weblinkpilot.shared.contracts;

public record AnalyticsBucketStatResponse(
    String bucket, long totalClicks, long redirectClicks, long qrScans, long uniqueVisitors) {}
