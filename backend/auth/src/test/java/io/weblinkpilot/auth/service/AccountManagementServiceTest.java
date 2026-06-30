package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.SocialLoginProvider;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.mapper.AuthResponseMapper;
import io.weblinkpilot.auth.repository.SocialIdentityRepository;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.auth.token.RefreshTokenService;
import io.weblinkpilot.shared.api.auth.AccountProfileResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private SocialIdentityRepository socialIdentityRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private UserAccountService userAccountService;

  private AccountManagementService service;

  @BeforeEach
  void setUp() {
    service =
        new AccountManagementService(
            userAccountRepository,
            socialIdentityRepository,
            passwordEncoder,
            refreshTokenService,
            userAccountService,
            new AuthResponseMapper());
  }

  @Test
  void profileReturnsAccountDetailsAndLinkedProviders() {
    UserAccount account =
        new UserAccount("alice", "hash", "alice@example.com", new Role("USER"), true, now(), now());
    account.markLoggedIn(now());
    when(userAccountService.getRequiredUser("alice")).thenReturn(account);
    when(socialIdentityRepository.findAllByUsername("alice"))
        .thenReturn(
            List.of(
                new SocialIdentity(
                    SocialLoginProvider.GITHUB, "123", "alice-github", account, now(), now())));

    AccountProfileResponse response = service.profile("alice");

    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
    assertThat(response.email()).isEqualTo("alice@example.com");
    assertThat(response.emailVerified()).isTrue();
    assertThat(response.socialIdentities()).hasSize(1);
    assertThat(response.socialIdentities().get(0).provider()).isEqualTo("GITHUB");
    assertThat(response.socialIdentities().get(0).providerLogin()).isEqualTo("alice-github");
  }

  @Test
  void changePasswordUpdatesHashAndRevokesSessions() {
    UserAccount account =
        new UserAccount(
            "alice", "old-hash", "alice@example.com", new Role("USER"), true, now(), null);
    when(userAccountService.getRequiredUser("alice")).thenReturn(account);
    when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
    when(passwordEncoder.encode("Newpass1")).thenReturn("new-hash");

    service.changePassword("alice", "old-password", "Newpass1");

    assertThat(account.getPasswordHash()).isEqualTo("new-hash");
    verify(userAccountRepository).save(account);
    verify(refreshTokenService).revokeAllForUser("alice");
  }

  @Test
  void changePasswordRejectsWrongCurrentPassword() {
    UserAccount account =
        new UserAccount(
            "alice", "old-hash", "alice@example.com", new Role("USER"), true, now(), null);
    when(userAccountService.getRequiredUser("alice")).thenReturn(account);
    when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

    assertThatThrownBy(() -> service.changePassword("alice", "wrong-password", "Newpass1"))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(userAccountRepository, never()).save(account);
    verify(refreshTokenService, never()).revokeAllForUser("alice");
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
