package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
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

  @Test
  void registerIssuesTokenForCreatedAccount() {
    AuthService service = new AuthService(userAccountService, jwtService);
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    when(userAccountService.registerUser("alice", "Password1")).thenReturn(account);
    when(jwtService.issueToken("alice", "USER")).thenReturn("token-1");

    AuthResponse response = service.register(new AuthCredentialsRequest("alice", "Password1"));

    assertThat(response.token()).isEqualTo("token-1");
    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void loginIssuesTokenForAuthenticatedAccount() {
    AuthService service = new AuthService(userAccountService, jwtService);
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    when(userAccountService.authenticate("alice", "Password1")).thenReturn(account);
    when(jwtService.issueToken("alice", "USER")).thenReturn("token-2");

    AuthResponse response = service.login(new AuthCredentialsRequest("alice", "Password1"));

    assertThat(response.token()).isEqualTo("token-2");
    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void profileReturnsCurrentUserProfile() {
    AuthService service = new AuthService(userAccountService, jwtService);
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
    AuthService service = new AuthService(userAccountService, jwtService);

    assertThatThrownBy(() -> service.profile(null))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Authentication required");
  }

  @Test
  void profileRejectsAnonymousAuthentication() {
    AuthService service = new AuthService(userAccountService, jwtService);

    assertThatThrownBy(
            () ->
                service.profile(
                    new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", List.of())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Authentication required");
  }
}
