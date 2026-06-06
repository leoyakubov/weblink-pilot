package io.weblinkpilot.auth.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountNotificationEventListener {

  private final AccountNotificationService notificationService;

  public AccountNotificationEventListener(AccountNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPasswordResetLinkRequested(PasswordResetLinkRequestedEvent event) {
    notificationService.sendPasswordResetLink(event.email(), event.link());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailVerificationLinkRequested(EmailVerificationLinkRequestedEvent event) {
    notificationService.sendEmailVerificationLink(event.email(), event.link());
  }
}
