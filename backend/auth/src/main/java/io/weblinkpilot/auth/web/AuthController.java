package io.weblinkpilot.auth.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.weblinkpilot.auth.service.AccountManagementService;
import io.weblinkpilot.auth.service.AuthCookieService;
import io.weblinkpilot.auth.service.AuthFrontendRedirectService;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.auth.service.AuthService.AuthSession;
import io.weblinkpilot.auth.service.GitHubOAuthService;
import io.weblinkpilot.auth.service.OAuthLoginService;
import io.weblinkpilot.shared.contracts.AccountProfileResponse;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.EmailVerificationConfirmRequest;
import io.weblinkpilot.shared.contracts.EmailVerificationRequest;
import io.weblinkpilot.shared.contracts.OAuthLoginCompleteRequest;
import io.weblinkpilot.shared.contracts.PasswordChangeRequest;
import io.weblinkpilot.shared.contracts.PasswordResetConfirmRequest;
import io.weblinkpilot.shared.contracts.PasswordResetRequest;
import io.weblinkpilot.shared.contracts.RefreshTokenRequest;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final AccountManagementService accountManagementService;
  private final AuthCookieService authCookieService;
  private final GitHubOAuthService gitHubOAuthService;
  private final OAuthLoginService oauthLoginService;
  private final AuthFrontendRedirectService authFrontendRedirectService;

  public AuthController(
      AuthService authService,
      AccountManagementService accountManagementService,
      AuthCookieService authCookieService,
      GitHubOAuthService gitHubOAuthService,
      OAuthLoginService oauthLoginService,
      AuthFrontendRedirectService authFrontendRedirectService) {
    this.authService = authService;
    this.accountManagementService = accountManagementService;
    this.authCookieService = authCookieService;
    this.gitHubOAuthService = gitHubOAuthService;
    this.oauthLoginService = oauthLoginService;
    this.authFrontendRedirectService = authFrontendRedirectService;
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
  public ResponseEntity<Void> register(
      @Valid @org.springframework.web.bind.annotation.RequestBody AuthCredentialsRequest request) {
    authService.register(request);
    return ResponseEntity.noContent().build();
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
      responses = {@ApiResponse(responseCode = "204", description = "Reset link queued")})
  public ResponseEntity<Void> requestPasswordReset(
      @Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetRequest request) {
    log.debug("auth.password-reset.request received email={}", request.email());
    authService.requestPasswordReset(request.email());
    return ResponseEntity.noContent().build();
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
      responses = {@ApiResponse(responseCode = "204", description = "Verification link queued")})
  public ResponseEntity<Void> requestEmailVerification(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          EmailVerificationRequest request) {
    log.debug("auth.email-verification.request received email={}", request.email());
    authService.requestEmailVerification(request.email());
    return ResponseEntity.noContent().build();
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

  @GetMapping("/oauth2/github/start")
  @Operation(summary = "Start GitHub login")
  public ResponseEntity<Void> startGithubLogin(HttpServletRequest request) {
    String state = gitHubOAuthService.createStateToken();
    String redirectUri = buildGithubCallbackUri(request);
    String authorizationUrl = gitHubOAuthService.buildAuthorizationUrl(redirectUri, state);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, authCookieService.createGithubStateCookie(state).toString())
        .location(URI.create(authorizationUrl))
        .build();
  }

  @GetMapping("/oauth2/github/callback")
  @Operation(summary = "Handle GitHub login callback")
  public ResponseEntity<Void> handleGithubCallback(
      @RequestParam String code, @RequestParam String state, HttpServletRequest request) {
    String cookieState = resolveGithubState(request);
    if (!state.equals(cookieState)) {
      throw new IllegalArgumentException("Invalid OAuth state");
    }

    String redirectUri = buildGithubCallbackUri(request);
    String ticket = gitHubOAuthService.completeLogin(code, redirectUri);
    URI frontendCompleteUri = authFrontendRedirectService.buildGithubCompleteUri(ticket);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, authCookieService.clearGithubStateCookie().toString())
        .location(frontendCompleteUri)
        .build();
  }

  @PostMapping("/oauth2/github/complete")
  @Operation(summary = "Complete GitHub login with the one-time ticket")
  public ResponseEntity<AuthResponse> completeGithubLogin(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          OAuthLoginCompleteRequest request) {
    OAuthLoginService.AuthSession session = oauthLoginService.completeLogin(request.ticket());
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
        .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshTokenCookie().toString())
        .build();
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

  private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
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

  private String resolveGithubState(HttpServletRequest request) {
    if (request.getCookies() == null) {
      throw new IllegalArgumentException("OAuth state is required");
    }
    for (var cookie : request.getCookies()) {
      if (cookie != null && cookie.getName().equals(authCookieService.getGithubStateCookieName())) {
        String value = cookie.getValue();
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    throw new IllegalArgumentException("OAuth state is required");
  }

  private String buildGithubCallbackUri(HttpServletRequest request) {
    return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
        .replacePath("/api/v1/auth/oauth2/github/callback")
        .replaceQuery(null)
        .build()
        .toUriString();
  }
}
