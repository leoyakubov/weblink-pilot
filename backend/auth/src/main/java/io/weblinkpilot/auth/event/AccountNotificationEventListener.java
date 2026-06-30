package io.weblinkpilot.auth.event;

import io.weblinkpilot.auth.notification.AccountNotificationService;
import io.weblinkpilot.auth.support.SafeLogValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountNotificationEventListener {

  private static final Logger log = LoggerFactory.getLogger(AccountNotificationEventListener.class);

  private final AccountNotificationService notificationService;

  public AccountNotificationEventListener(AccountNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onPasswordResetLinkRequested(PasswordResetLinkRequestedEvent event) {
    log.debug(
        "auth.mail.worker.hand-off type=password-reset email={}",
        SafeLogValue.email(event.email()));
    notificationService.sendPasswordResetLink(event.email(), event.link());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailVerificationLinkRequested(EmailVerificationLinkRequestedEvent event) {
    log.debug(
        "auth.mail.worker.hand-off type=email-verification email={}",
        SafeLogValue.email(event.email()));
    notificationService.sendEmailVerificationLink(event.email(), event.link());
  }
}
