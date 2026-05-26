package io.weblinkpilot.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailAccountNotificationService implements AccountNotificationService {

  private static final String FROM_ADDRESS = "no-reply@weblinkpilot.local";
  private static final String FROM_NAME = "WebLinkPilot";

  private final JavaMailSender mailSender;

  public MailAccountNotificationService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void sendPasswordResetLink(String email, String link) {
    send(email, "Reset your WebLinkPilot password", buildBody("password reset", link));
  }

  @Override
  public void sendEmailVerificationLink(String email, String link) {
    send(email, "Verify your WebLinkPilot email", buildBody("email verification", link));
  }

  private void send(String email, String subject, String body) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(FROM_ADDRESS, FROM_NAME);
      helper.setTo(email);
      helper.setSubject(subject);
      helper.setText(body, false);
      mailSender.send(message);
    } catch (MessagingException exception) {
      throw new IllegalStateException("Unable to send account notification email", exception);
    } catch (UnsupportedEncodingException exception) {
      throw new IllegalStateException("Unable to format account notification email", exception);
    }
  }

  private String buildBody(String action, String link) {
    return """
        Hello,

        We received a %s request for your WebLinkPilot account.

        Open this link to continue:
        %s

        If you did not request this, you can ignore this email.
        """
        .formatted(action, link);
  }
}
