package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.auth.service.AccountManagementService;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.shared.api.auth.AccountProfileResponse;
import io.weblinkpilot.shared.api.auth.PasswordChangeRequest;
import io.weblinkpilot.shared.api.auth.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthAccountController {

  private final AuthService authService;
  private final AccountManagementService accountManagementService;

  public AuthAccountController(
      AuthService authService, AccountManagementService accountManagementService) {
    this.authService = authService;
    this.accountManagementService = accountManagementService;
  }

  @GetMapping("/me")
  @Operation(summary = "Current signed-in user")
  @SecurityRequirement(name = "bearerAuth")
  public UserProfileResponse me(Authentication authentication) {
    return authService.profile(authentication);
  }

  @GetMapping("/account")
  @Operation(summary = "Current signed-in account details")
  @SecurityRequirement(name = "bearerAuth")
  public AccountProfileResponse account(Authentication authentication) {
    return accountManagementService.profile(authentication.getName());
  }

  @PostMapping("/account/password")
  @Operation(
      summary = "Change the current account password",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PasswordChangeRequest.class))),
      responses = {@ApiResponse(responseCode = "204", description = "Password updated")})
  public ResponseEntity<Void> changePassword(
      Authentication authentication,
      @Valid @org.springframework.web.bind.annotation.RequestBody PasswordChangeRequest request) {
    accountManagementService.changePassword(
        authentication.getName(), request.currentPassword(), request.newPassword());
    return ResponseEntity.noContent().build();
  }
}
