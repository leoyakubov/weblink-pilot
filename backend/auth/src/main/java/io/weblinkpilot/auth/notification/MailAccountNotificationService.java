package io.weblinkpilot.auth.notification;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.MailProperties;
import io.weblinkpilot.auth.notification.template.AccountNotificationEmail;
import io.weblinkpilot.auth.notification.template.AccountNotificationTemplateRenderer;
import io.weblinkpilot.auth.support.SafeLogValue;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this service.")
public class MailAccountNotificationService implements AccountNotificationService {

  private static final Logger log = LoggerFactory.getLogger(MailAccountNotificationService.class);

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;
  private final AccountNotificationTemplateRenderer templateRenderer;

  public MailAccountNotificationService(
      JavaMailSender mailSender,
      MailProperties mailProperties,
      AccountNotificationTemplateRenderer templateRenderer) {
    this.mailSender = mailSender;
    this.mailProperties = mailProperties;
    this.templateRenderer = templateRenderer;
  }

  @Override
  public void sendPasswordResetLink(String email, String link) {
    send(email, templateRenderer.passwordReset(link));
  }

  @Override
  public void sendEmailVerificationLink(String email, String link) {
    send(email, templateRenderer.emailVerification(link));
  }

  private void send(String email, AccountNotificationEmail messageTemplate) {
    String subject = messageTemplate.subject();
    try {
      log.debug("auth.mail.sending to={} subject={}", SafeLogValue.email(email), subject);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
      helper.setTo(email);
      helper.setSubject(subject);
      helper.setText(messageTemplate.body(), false);
      mailSender.send(message);
      log.info("auth.mail.sent to={} subject={}", SafeLogValue.email(email), subject);
    } catch (MessagingException exception) {
      log.error("auth.mail.failed to={} subject={}", SafeLogValue.email(email), subject, exception);
      throw new IllegalStateException("Unable to send account notification email", exception);
    } catch (UnsupportedEncodingException exception) {
      log.error(
          "auth.mail.format-failed to={} subject={}",
          SafeLogValue.email(email),
          subject,
          exception);
      throw new IllegalStateException("Unable to format account notification email", exception);
    }
  }
}
