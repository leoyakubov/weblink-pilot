package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.session.AuthSession;
import io.weblinkpilot.auth.token.JwtService;
import io.weblinkpilot.auth.token.RefreshTokenService;
import io.weblinkpilot.shared.api.auth.AuthCredentialsRequest;
import io.weblinkpilot.shared.api.auth.UserProfileResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserAccountService userAccountService;

  @Mock private JwtService jwtService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private PasswordResetService passwordResetService;

  @Mock private EmailVerificationService emailVerificationService;

  @Test
  void registerCreatesAccountAndRequestsVerification() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(userAccountService.registerUser("alice", "Password1", "alice@example.com"))
        .thenReturn(account);

    service.register(new AuthCredentialsRequest("alice", "Password1", "alice@example.com"));

    verify(emailVerificationService).requestEmailVerification("alice@example.com");
  }

  @Test
  void loginIssuesTokenForAuthenticatedAccount() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(userAccountService.authenticate("alice", "Password1")).thenReturn(account);
    when(jwtService.issueToken("alice", "USER")).thenReturn("token-2");
    when(refreshTokenService.issueRefreshToken(account)).thenReturn("refresh-2");

    AuthSession response = service.login(new AuthCredentialsRequest("alice", "Password1"));

    assertThat(response.token()).isEqualTo("token-2");
    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
    assertThat(response.refreshToken()).isEqualTo("refresh-2");
  }

  @Test
  void refreshRotatesRefreshTokenForCurrentAccount() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(refreshTokenService.rotateRefreshToken("refresh-1"))
        .thenReturn(new RefreshTokenService.RotationResult("alice", "USER", "refresh-2"));
    when(jwtService.issueToken("alice", "USER")).thenReturn("token-3");

    AuthSession response = service.refresh("refresh-1");

    assertThat(response.token()).isEqualTo("token-3");
    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
    assertThat(response.refreshToken()).isEqualTo("refresh-2");
    verify(refreshTokenService).rotateRefreshToken("refresh-1");
  }

  @Test
  void logoutRevokesRefreshToken() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);

    service.logout("refresh-1");

    verify(refreshTokenService).revokeRefreshToken("refresh-1");
  }

  @Test
  void profileReturnsCurrentUserProfile() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);
    when(userAccountService.profile("alice")).thenReturn(new UserProfileResponse("alice", "USER"));

    UserProfileResponse response =
        service.profile(
            new UsernamePasswordAuthenticationToken(
                "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void profileRejectsMissingAuthentication() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);

    assertThatThrownBy(() -> service.profile(null))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Authentication required");
  }

  @Test
  void profileRejectsAnonymousAuthentication() {
    AuthService service =
        new AuthService(
            userAccountService,
            jwtService,
            refreshTokenService,
            passwordResetService,
            emailVerificationService);

    assertThatThrownBy(
            () ->
                service.profile(
                    new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", List.of())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Authentication required");
  }
}
