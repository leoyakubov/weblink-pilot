package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.shared.api.auth.AccountActionPreviewResponse;
import io.weblinkpilot.shared.api.auth.AuthCredentialsRequest;
import io.weblinkpilot.shared.api.auth.EmailVerificationConfirmRequest;
import io.weblinkpilot.shared.api.auth.EmailVerificationRequest;
import io.weblinkpilot.shared.api.auth.PasswordResetConfirmRequest;
import io.weblinkpilot.shared.api.auth.PasswordResetRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @Operation(
      summary = "Register a user",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AuthCredentialsRequest.class))),
      responses = {@ApiResponse(responseCode = "200", description = "Preview link returned")})
  public ResponseEntity<AccountActionPreviewResponse> register(
      @Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
    String previewLink = authService.register(request);
    return preview(previewLink);
  }

  @PostMapping("/password-reset/request")
  @Operation(
      summary = "Request a password reset link",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PasswordResetRequest.class))),
      responses = {@ApiResponse(responseCode = "200", description = "Preview link returned")})
  public ResponseEntity<AccountActionPreviewResponse> requestPasswordReset(
      @Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetRequest request) {
    return preview(authService.requestPasswordReset(request.email()));
  }

  @PostMapping("/password-reset/confirm")
  @Operation(
      summary = "Confirm a password reset",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PasswordResetConfirmRequest.class))),
      responses = {@ApiResponse(responseCode = "204", description = "Password updated")})
  public ResponseEntity<Void> confirmPasswordReset(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          PasswordResetConfirmRequest request) {
    authService.confirmPasswordReset(request.token(), request.password());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/email-verification/request")
  @Operation(
      summary = "Request an email verification link",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = EmailVerificationRequest.class))),
      responses = {@ApiResponse(responseCode = "200", description = "Preview link returned")})
  public ResponseEntity<AccountActionPreviewResponse> requestEmailVerification(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          EmailVerificationRequest request) {
    return preview(authService.requestEmailVerification(request.email()));
  }

  @PostMapping("/email-verification/confirm")
  @Operation(
      summary = "Confirm an email verification link",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = EmailVerificationConfirmRequest.class))),
      responses = {@ApiResponse(responseCode = "204", description = "Email verified")})
  public ResponseEntity<Void> confirmEmailVerification(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          EmailVerificationConfirmRequest request) {
    authService.confirmEmailVerification(request.token());
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<AccountActionPreviewResponse> preview(String previewLink) {
    return ResponseEntity.ok(
        new AccountActionPreviewResponse(authService.isDemoMailboxEnabled() ? previewLink : null));
  }
}
