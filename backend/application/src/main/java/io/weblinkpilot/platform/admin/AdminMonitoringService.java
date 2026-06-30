package io.weblinkpilot.platform.admin;

import io.weblinkpilot.shared.api.admin.AdminMonitoringResponse;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitoringService {

  private final AdminRuntimeMetricsService runtimeMetricsService;
  private final AdminHealthService healthService;
  private final AdminConfigurationSnapshotService configurationSnapshotService;

  public AdminMonitoringService(
      AdminRuntimeMetricsService runtimeMetricsService,
      AdminHealthService healthService,
      AdminConfigurationSnapshotService configurationSnapshotService) {
    this.runtimeMetricsService = runtimeMetricsService;
    this.healthService = healthService;
    this.configurationSnapshotService = configurationSnapshotService;
  }

  public AdminMonitoringResponse snapshot() {
    return new AdminMonitoringResponse(
        runtimeMetricsService.metrics(),
        healthService.health(),
        configurationSnapshotService.configuration());
  }
}
