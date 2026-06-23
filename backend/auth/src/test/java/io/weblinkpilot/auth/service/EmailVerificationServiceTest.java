package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  @Mock private UserAccountRepository userAccountRepository;

  @Mock private AccountActionTokenService tokenService;

  @Mock private AccountActionRequestCooldownService cooldownService;

  @Mock private ApplicationEventPublisher eventPublisher;

  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setFrontendBaseUrl("http://localhost:8081");
    authProperties.setAccountActionTokenTtlHours(24);
    service =
        new EmailVerificationService(
            userAccountRepository, tokenService, cooldownService, eventPublisher, authProperties);
  }

  @Test
  void requestEmailVerificationPublishesNotificationEventForUnverifiedAccount() {
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
    when(tokenService.issueToken(account, AccountActionTokenType.EMAIL_VERIFICATION))
        .thenReturn("verification-token");

    assertThat(service.requestEmailVerification("alice@example.com"))
        .isEqualTo("http://localhost:8081/auth/verify-email?token=verification-token");

    verify(cooldownService).enforceCooldown("verification email", "alice@example.com");
    ArgumentCaptor<EmailVerificationLinkRequestedEvent> eventCaptor =
        ArgumentCaptor.forClass(EmailVerificationLinkRequestedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().email()).isEqualTo("alice@example.com");
    assertThat(eventCaptor.getValue().link()).contains("/auth/verify-email?token=");
  }

  @Test
  void requestEmailVerificationSkipsVerifiedAccount() {
    UserAccount account =
        new UserAccount(
            "alice",
            "hashed",
            "alice@example.com",
            new Role("USER"),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC));
    when(userAccountRepository.findByEmailIgnoreCase("alice@example.com"))
        .thenReturn(Optional.of(account));

    assertThat(service.requestEmailVerification("alice@example.com")).isNull();

    verify(cooldownService, never()).enforceCooldown(any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void confirmEmailVerificationMarksAccountVerified() {
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
            AccountActionTokenType.EMAIL_VERIFICATION,
            account,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
    when(tokenService.consumeToken("verification-token", AccountActionTokenType.EMAIL_VERIFICATION))
        .thenReturn(Optional.of(token));
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.confirmEmailVerification("verification-token");

    verify(userAccountRepository).save(account);
    assertThat(account.isEmailVerified()).isTrue();
  }

  @Test
  void confirmEmailVerificationRejectsMissingToken() {
    assertThatThrownBy(() -> service.confirmEmailVerification(""))
        .isInstanceOf(io.weblinkpilot.auth.exception.InvalidAccountActionTokenException.class);
  }
}
