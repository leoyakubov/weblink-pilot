package io.weblinkpilot.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailAccountNotificationService implements AccountNotificationService {

  private static final String FROM_ADDRESS = "no-reply@weblinkpilot.local";
  private static final String FROM_NAME = "WebLinkPilot";
  private static final Logger log = LoggerFactory.getLogger(MailAccountNotificationService.class);

  private final JavaMailSender mailSender;

  public MailAccountNotificationService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void sendPasswordResetLink(String email, String link) {
    send(
        email,
        "Reset your WebLinkPilot password",
        """
            Hi,

            We received a request to reset the password for your WebLinkPilot account.

            Reset your password here:
            %s

            If you did not ask for this, you can safely ignore this message.

            Thanks,
            The WebLinkPilot team
            """
            .formatted(link));
  }

  @Override
  public void sendEmailVerificationLink(String email, String link) {
    send(
        email,
        "Verify your WebLinkPilot email",
        """
            Hi,

            Welcome to WebLinkPilot. To finish creating your account, please verify your email address.

            Verify your email here:
            %s

            If you did not create this account, you can ignore this email.

            Thanks,
            The WebLinkPilot team
            """
            .formatted(link));
  }

  private void send(String email, String subject, String body) {
    try {
      log.debug("auth.mail.sending to={} subject={}", email, subject);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(FROM_ADDRESS, FROM_NAME);
      helper.setTo(email);
      helper.setSubject(subject);
      helper.setText(body, false);
      mailSender.send(message);
      log.info("auth.mail.sent to={} subject={}", email, subject);
    } catch (MessagingException exception) {
      log.error("auth.mail.failed to={} subject={}", email, subject, exception);
      throw new IllegalStateException("Unable to send account notification email", exception);
    } catch (UnsupportedEncodingException exception) {
      log.error("auth.mail.format-failed to={} subject={}", email, subject, exception);
      throw new IllegalStateException("Unable to format account notification email", exception);
    }
  }
}
