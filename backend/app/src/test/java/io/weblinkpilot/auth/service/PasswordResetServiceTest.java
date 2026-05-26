package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionToken;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private UserAccountRepository userAccountRepository;

  @Mock private UserAccountService userAccountService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private AccountActionTokenService tokenService;

  @Mock private AccountNotificationService notificationService;

  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setFrontendBaseUrl("http://localhost:8081");
    authProperties.setAccountActionTokenTtlHours(24);
    service =
        new PasswordResetService(
            userAccountRepository,
            userAccountService,
            passwordEncoder,
            tokenService,
            notificationService,
            authProperties);
  }

  @Test
  void requestPasswordResetSendsNotificationForKnownEmail() {
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(userAccountRepository.findByEmailIgnoreCase("alice@example.com"))
        .thenReturn(Optional.of(account));
    when(tokenService.issueToken(account, AccountActionTokenType.PASSWORD_RESET))
        .thenReturn("reset-token");

    service.requestPasswordReset("alice@example.com");

    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendPasswordResetLink(emailCaptor.capture(), linkCaptor.capture());
    assertThat(emailCaptor.getValue()).isEqualTo("alice@example.com");
    assertThat(linkCaptor.getValue()).contains("/auth/reset-password?token=");
  }

  @Test
  void confirmPasswordResetUpdatesPassword() {
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    AccountActionToken token =
        new AccountActionToken(
            "hash",
            AccountActionTokenType.PASSWORD_RESET,
            account,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
    when(tokenService.consumeToken("reset-token", AccountActionTokenType.PASSWORD_RESET))
        .thenReturn(Optional.of(token));
    when(passwordEncoder.encode("Password1")).thenReturn("encoded");
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.confirmPasswordReset("reset-token", "Password1");

    verify(userAccountRepository).save(account);
    assertThat(account.getPasswordHash()).isEqualTo("encoded");
  }
}
