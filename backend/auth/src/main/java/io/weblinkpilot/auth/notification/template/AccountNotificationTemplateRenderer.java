package io.weblinkpilot.auth.notification.template;

import org.springframework.stereotype.Component;

@Component
public class AccountNotificationTemplateRenderer {

  public AccountNotificationEmail passwordReset(String link) {
    return new AccountNotificationEmail(
        "Reset your WeblinkPilot password",
        String.join(
            System.lineSeparator(),
            "Hi,",
            "",
            "We received a request to reset the password for your WeblinkPilot account.",
            "",
            "Reset your password here:",
            link,
            "",
            "If you did not ask for this, you can safely ignore this message.",
            "",
            "Thanks,",
            "The WeblinkPilot team"));
  }

  public AccountNotificationEmail emailVerification(String link) {
    return new AccountNotificationEmail(
        "Verify your WeblinkPilot email",
        String.join(
            System.lineSeparator(),
            "Hi,",
            "",
            "Welcome to WeblinkPilot. To finish creating your account, please verify your email address.",
            "",
            "Verify your email here:",
            link,
            "",
            "If you did not create this account, you can ignore this email.",
            "",
            "Thanks,",
            "The WeblinkPilot team"));
  }
}
