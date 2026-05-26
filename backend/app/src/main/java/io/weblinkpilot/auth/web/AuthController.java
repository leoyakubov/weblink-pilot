package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.auth.service.AuthService.AuthSession;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.RefreshTokenRequest;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final AuthCookieService authCookieService;

  public AuthController(AuthService authService, AuthCookieService authCookieService) {
    this.authService = authService;
    this.authCookieService = authCookieService;
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
                      schema = @Schema(implementation = AuthCredentialsRequest.class))))
  public ResponseEntity<AuthResponse> register(
      @Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
    AuthSession session = authService.register(request);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            authCookieService.createRefreshTokenCookie(session.refreshToken()).toString())
        .body(new AuthResponse(session.token(), session.username(), session.role()));
  }

  @PostMapping("/login")
  @Operation(
      summary = "Login and receive an access token plus refresh cookie",
      requestBody =
          @RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AuthCredentialsRequest.class))),
      responses = {@ApiResponse(responseCode = "200", description = "JWT issued")})
  public ResponseEntity<AuthResponse> login(
      @Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
    AuthSession session = authService.login(request);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            authCookieService.createRefreshTokenCookie(session.refreshToken()).toString())
        .body(new AuthResponse(session.token(), session.username(), session.role()));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Rotate the refresh token cookie",
      requestBody =
          @RequestBody(
              required = false,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = RefreshTokenRequest.class))),
      responses = {@ApiResponse(responseCode = "200", description = "Tokens rotated")})
  public ResponseEntity<AuthResponse> refresh(
      @Valid @org.springframework.web.bind.annotation.RequestBody(required = false)
          RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    String refreshToken = resolveRefreshToken(request, httpRequest);
    AuthSession session = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            authCookieService.createRefreshTokenCookie(session.refreshToken()).toString())
        .body(new AuthResponse(session.token(), session.username(), session.role()));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Revoke the refresh token cookie",
      requestBody =
          @RequestBody(
              required = false,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = RefreshTokenRequest.class))),
      responses = {@ApiResponse(responseCode = "204", description = "Session revoked")})
  public ResponseEntity<Void> logout(
      @Valid @org.springframework.web.bind.annotation.RequestBody(required = false)
          RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    String refreshToken = resolveRefreshToken(request, httpRequest);
    authService.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(
            HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString())
        .build();
  }

  @GetMapping("/me")
  @Operation(summary = "Current signed-in user")
  @SecurityRequirement(name = "bearerAuth")
  public UserProfileResponse me(Authentication authentication) {
    return authService.profile(authentication);
  }

  private String resolveRefreshToken(
      RefreshTokenRequest request, HttpServletRequest httpRequest) {
    if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
      return request.refreshToken();
    }
    if (httpRequest.getCookies() == null) {
      throw new IllegalArgumentException("Refresh token is required");
    }
    for (var cookie : httpRequest.getCookies()) {
      if (cookie != null && cookie.getName().equals(authCookieService.getCookieName())) {
        String value = cookie.getValue();
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    throw new IllegalArgumentException("Refresh token is required");
  }
}
