package io.weblinkpilot.auth.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.service.AuthCookieService;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.auth.service.AuthService.AuthSession;
import io.weblinkpilot.auth.service.GitHubOAuthService;
import io.weblinkpilot.auth.service.OAuthLoginService;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;
  @Mock private GitHubOAuthService gitHubOAuthService;
  @Mock private OAuthLoginService oauthLoginService;

  private MockMvc mockMvc;
  private AuthCookieService authCookieService;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setRefreshCookieName("weblinkpilot_refresh");
    authProperties.setRefreshCookiePath("/api/v1/auth");
    authProperties.setRefreshCookieSameSite("Lax");
    authProperties.setRefreshCookieSecure(false);
    authProperties.setRefreshTokenTtlDays(30);
    authProperties.setGithubStateCookieName("weblinkpilot_github_oauth_state");
    authProperties.setGithubStateCookiePath("/api/v1/auth/oauth2/github");
    authProperties.setGithubLoginTicketTtlMinutes(10);
    authProperties.setFrontendBaseUrl("http://localhost:8081");
    authCookieService = new AuthCookieService(authProperties);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new AuthController(
                    authService,
                    authCookieService,
                    gitHubOAuthService,
                    oauthLoginService,
                    authProperties))
            .build();
  }

  @Test
  void registerSetsRefreshCookieAndReturnsAccessTokenOnly() throws Exception {
    when(authService.register(
            new AuthCredentialsRequest("alice", "Password1", "alice@example.com")))
        .thenReturn(new AuthSession("token-1", "refresh-1", "alice", "USER"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"alice","password":"Password1","email":"alice@example.com"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-1"))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("weblinkpilot_refresh=refresh-1"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Path=/api/v1/auth"))));
  }

  @Test
  void refreshReadsRefreshTokenFromCookie() throws Exception {
    when(authService.refresh("refresh-1"))
        .thenReturn(new AuthSession("token-2", "refresh-2", "alice", "USER"));

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("weblinkpilot_refresh", "refresh-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-2"))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("weblinkpilot_refresh=refresh-2")));

    verify(authService).refresh("refresh-1");
  }

  @Test
  void logoutClearsRefreshCookie() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("weblinkpilot_refresh", "refresh-1")))
        .andExpect(status().isNoContent())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("weblinkpilot_refresh="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"))));

    verify(authService).logout("refresh-1");
  }

  @Test
  void requestPasswordResetReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"alice@example.com"}
                    """))
        .andExpect(status().isNoContent());

    verify(authService).requestPasswordReset("alice@example.com");
  }

  @Test
  void confirmPasswordResetReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"token":"reset-token","password":"Password1"}
                    """))
        .andExpect(status().isNoContent());

    verify(authService).confirmPasswordReset("reset-token", "Password1");
  }

  @Test
  void requestEmailVerificationReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/email-verification/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"alice@example.com"}
                    """))
        .andExpect(status().isNoContent());

    verify(authService).requestEmailVerification("alice@example.com");
  }

  @Test
  void confirmEmailVerificationReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/email-verification/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"token":"verification-token"}
                    """))
        .andExpect(status().isNoContent());

    verify(authService).confirmEmailVerification("verification-token");
  }

  @Test
  void startGithubLoginRedirectsToGithubAndSetsStateCookie() throws Exception {
    when(gitHubOAuthService.createStateToken()).thenReturn("state-token");
    when(gitHubOAuthService.buildAuthorizationUrl(
            "http://localhost/api/v1/auth/oauth2/github/callback", "state-token"))
        .thenReturn("https://github.com/login/oauth/authorize?state=state-token");

    mockMvc
        .perform(get("/api/v1/auth/oauth2/github/start"))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string("Location", "https://github.com/login/oauth/authorize?state=state-token"))
        .andExpect(cookie().value("weblinkpilot_github_oauth_state", "state-token"));
  }

  @Test
  void completeGithubCallbackRedirectsToFrontendCompleteRoute() throws Exception {
    when(gitHubOAuthService.completeLogin(
            "github-code", "http://localhost/api/v1/auth/oauth2/github/callback"))
        .thenReturn("login-ticket");

    mockMvc
        .perform(
            get("/api/v1/auth/oauth2/github/callback")
                .param("code", "github-code")
                .param("state", "state-token")
                .cookie(
                    new jakarta.servlet.http.Cookie(
                        "weblinkpilot_github_oauth_state", "state-token")))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    "Location", "http://localhost:8081/auth/github/complete#ticket=login-ticket"))
        .andExpect(cookie().value("weblinkpilot_github_oauth_state", ""));
  }

  @Test
  void completeGithubLoginReturnsAccessTokenAndRefreshCookie() throws Exception {
    when(oauthLoginService.completeLogin("login-ticket"))
        .thenReturn(new OAuthLoginService.AuthSession("token-9", "refresh-9", "alice", "USER"));

    mockMvc
        .perform(
            post("/api/v1/auth/oauth2/github/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticket\":\"login-ticket\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-9"))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("weblinkpilot_refresh=refresh-9")));
  }
}
