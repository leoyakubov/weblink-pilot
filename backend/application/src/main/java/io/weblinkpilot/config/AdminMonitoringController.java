package io.weblinkpilot.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.shared.contracts.AdminMonitoringResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMonitoringController {

  private final AdminMonitoringService adminMonitoringService;

  public AdminMonitoringController(AdminMonitoringService adminMonitoringService) {
    this.adminMonitoringService = adminMonitoringService;
  }

  @GetMapping("/monitoring")
  @Operation(summary = "Admin monitoring snapshot")
  @SecurityRequirement(name = "bearerAuth")
  public AdminMonitoringResponse monitoring() {
    return adminMonitoringService.snapshot();
  }
}
