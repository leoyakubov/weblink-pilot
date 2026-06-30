package io.weblinkpilot.auth.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.weblinkpilot.auth.service.GitHubOAuthService;
import io.weblinkpilot.auth.service.OAuthLoginService;
import io.weblinkpilot.auth.web.support.AuthCookieRequestResolver;
import io.weblinkpilot.auth.web.support.AuthCookieService;
import io.weblinkpilot.auth.web.support.AuthFrontendRedirectService;
import io.weblinkpilot.auth.web.support.AuthSessionResponseFactory;
import io.weblinkpilot.shared.api.auth.AuthResponse;
import io.weblinkpilot.shared.api.auth.OAuthLoginCompleteRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth/oauth2/github")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2")
public class GitHubOAuthController {

  private final AuthCookieService authCookieService;
  private final AuthCookieRequestResolver cookieRequestResolver;
  private final AuthFrontendRedirectService authFrontendRedirectService;
  private final AuthSessionResponseFactory responseFactory;
  private final GitHubOAuthService gitHubOAuthService;
  private final OAuthLoginService oauthLoginService;

  public GitHubOAuthController(
      AuthCookieService authCookieService,
      AuthCookieRequestResolver cookieRequestResolver,
      AuthFrontendRedirectService authFrontendRedirectService,
      AuthSessionResponseFactory responseFactory,
      GitHubOAuthService gitHubOAuthService,
      OAuthLoginService oauthLoginService) {
    this.authCookieService = authCookieService;
    this.cookieRequestResolver = cookieRequestResolver;
    this.authFrontendRedirectService = authFrontendRedirectService;
    this.responseFactory = responseFactory;
    this.gitHubOAuthService = gitHubOAuthService;
    this.oauthLoginService = oauthLoginService;
  }

  @GetMapping("/start")
  @Operation(summary = "Start GitHub login")
  public ResponseEntity<Void> start(HttpServletRequest request) {
    if (!gitHubOAuthService.isConfigured()) {
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(authFrontendRedirectService.buildGithubErrorUri("github_not_configured"))
          .build();
    }

    String state = gitHubOAuthService.createStateToken();
    String redirectUri = buildCallbackUri(request);
    String authorizationUrl = gitHubOAuthService.buildAuthorizationUrl(redirectUri, state);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, authCookieService.createGithubStateCookie(state).toString())
        .location(URI.create(authorizationUrl))
        .build();
  }

  @GetMapping("/callback")
  @Operation(summary = "Handle GitHub login callback")
  public ResponseEntity<Void> callback(
      @RequestParam String code, @RequestParam String state, HttpServletRequest request) {
    String cookieState = cookieRequestResolver.resolveGithubState(request);
    if (!state.equals(cookieState)) {
      throw new IllegalArgumentException("Invalid OAuth state");
    }

    String ticket = gitHubOAuthService.completeLogin(code, buildCallbackUri(request));
    URI frontendCompleteUri = authFrontendRedirectService.buildGithubCompleteUri(ticket);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, authCookieService.clearGithubStateCookie().toString())
        .location(frontendCompleteUri)
        .build();
  }

  @PostMapping("/complete")
  @Operation(summary = "Complete GitHub login with the one-time ticket")
  public ResponseEntity<AuthResponse> complete(
      @Valid @org.springframework.web.bind.annotation.RequestBody
          OAuthLoginCompleteRequest request) {
    return responseFactory.authenticated(oauthLoginService.completeLogin(request.ticket()));
  }

  private String buildCallbackUri(HttpServletRequest request) {
    return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
        .replacePath("/api/v1/auth/oauth2/github/callback")
        .replaceQuery(null)
        .build()
        .toUriString();
  }
}
