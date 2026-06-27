package io.weblinkpilot.shared.contracts;

import java.util.List;

public record AdminMonitoringResponse(
    List<AdminRuntimeMetricResponse> metrics,
    List<AdminHealthComponentResponse> health,
    List<AdminConfigurationItemResponse> configuration) {}
