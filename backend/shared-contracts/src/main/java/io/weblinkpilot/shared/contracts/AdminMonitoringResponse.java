package io.weblinkpilot.shared.contracts;

import java.util.List;

public record AdminMonitoringResponse(
    List<AdminRuntimeMetricResponse> metrics,
    List<AdminHealthComponentResponse> health,
    List<AdminConfigurationItemResponse> configuration) {

  public AdminMonitoringResponse {
    metrics = metrics == null ? List.of() : List.copyOf(metrics);
    health = health == null ? List.of() : List.copyOf(health);
    configuration = configuration == null ? List.of() : List.copyOf(configuration);
  }
}
