package io.weblinkpilot.auth.notification.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountNotificationTemplateRendererTest {

  private final AccountNotificationTemplateRenderer renderer =
      new AccountNotificationTemplateRenderer();

  @Test
  void rendersPasswordResetEmailWithSharedLayout() {
    AccountNotificationEmail email = renderer.passwordReset("https://example.test/reset-token");

    assertThat(email.subject()).isEqualTo("Reset your WeblinkPilot password");
    assertThat(email.body()).startsWith("Hi,");
    assertThat(email.body()).contains("reset the password for your WeblinkPilot account");
    assertThat(email.body()).contains("https://example.test/reset-token");
    assertThat(email.body()).endsWith("Thanks,\nThe WeblinkPilot team");
  }

  @Test
  void rendersEmailVerificationEmailWithSharedLayout() {
    AccountNotificationEmail email =
        renderer.emailVerification("https://example.test/verify-token");

    assertThat(email.subject()).isEqualTo("Verify your WeblinkPilot email");
    assertThat(email.body()).startsWith("Hi,");
    assertThat(email.body()).contains("please verify your email address");
    assertThat(email.body()).contains("https://example.test/verify-token");
    assertThat(email.body()).endsWith("Thanks,\nThe WeblinkPilot team");
  }
}
