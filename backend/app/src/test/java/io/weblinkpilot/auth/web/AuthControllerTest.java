package io.weblinkpilot.auth.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.service.AuthService;
import io.weblinkpilot.auth.service.AuthService.AuthSession;
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
    authCookieService = new AuthCookieService(authProperties);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(authService, authCookieService)).build();
  }

  @Test
  void registerSetsRefreshCookieAndReturnsAccessTokenOnly() throws Exception {
    when(authService.register(new AuthCredentialsRequest("alice", "Password1")))
        .thenReturn(new AuthSession("token-1", "refresh-1", "alice", "USER"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"alice","password":"Password1"}
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
}
