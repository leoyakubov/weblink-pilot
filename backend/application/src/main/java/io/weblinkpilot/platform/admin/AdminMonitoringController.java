package io.weblinkpilot.platform.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.platform.web.ApiRoutes;
import io.weblinkpilot.shared.api.admin.AdminMonitoringResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.ADMIN_BASE)
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
