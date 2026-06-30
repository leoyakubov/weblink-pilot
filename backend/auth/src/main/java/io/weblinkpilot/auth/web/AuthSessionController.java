package io.weblinkpilot.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.auth.web.support.AuthCookieRequestResolver;
import io.weblinkpilot.auth.web.support.AuthSessionResponseFactory;
import io.weblinkpilot.shared.api.auth.AuthCredentialsRequest;
import io.weblinkpilot.shared.api.auth.AuthResponse;
import io.weblinkpilot.shared.api.auth.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthSessionController {

  private final AuthService authService;
  private final AuthCookieRequestResolver cookieRequestResolver;
  private final AuthSessionResponseFactory responseFactory;

  public AuthSessionController(
      AuthService authService,
      AuthCookieRequestResolver cookieRequestResolver,
      AuthSessionResponseFactory responseFactory) {
    this.authService = authService;
    this.cookieRequestResolver = cookieRequestResolver;
    this.responseFactory = responseFactory;
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
    return responseFactory.authenticated(authService.login(request));
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
    String refreshToken = cookieRequestResolver.resolveRefreshToken(request, httpRequest);
    return responseFactory.authenticated(authService.refresh(refreshToken));
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
    String refreshToken = cookieRequestResolver.resolveRefreshToken(request, httpRequest);
    authService.logout(refreshToken);
    return responseFactory.loggedOut();
  }
}
