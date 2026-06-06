package io.weblinkpilot.auth.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountNotificationEventListenerTest {

  @Mock private AccountNotificationService notificationService;

  private AccountNotificationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new AccountNotificationEventListener(notificationService);
  }

  @Test
  void forwardsPasswordResetEventsToNotificationService() {
    listener.onPasswordResetLinkRequested(
        new PasswordResetLinkRequestedEvent("alice@example.com", "https://example/reset"));

    verify(notificationService).sendPasswordResetLink("alice@example.com", "https://example/reset");
  }

  @Test
  void forwardsEmailVerificationEventsToNotificationService() {
    listener.onEmailVerificationLinkRequested(
        new EmailVerificationLinkRequestedEvent("alice@example.com", "https://example/verify"));

    verify(notificationService)
        .sendEmailVerificationLink("alice@example.com", "https://example/verify");
  }
}
