package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.auth.service.AdminOverviewService;
import io.weblinkpilot.shared.contracts.AdminOverviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminOverviewService adminOverviewService;

  public AdminController(AdminOverviewService adminOverviewService) {
    this.adminOverviewService = adminOverviewService;
  }

  @GetMapping("/overview")
  @Operation(summary = "Admin overview")
  @SecurityRequirement(name = "bearerAuth")
  public AdminOverviewResponse overview() {
    return adminOverviewService.overview();
  }
}
