package io.weblinkpilot.config;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev"})
public class MailServerStartupHealthCheck implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(MailServerStartupHealthCheck.class);

  private final JavaMailSenderImpl mailSender;

  public MailServerStartupHealthCheck(JavaMailSenderImpl mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      mailSender.testConnection();
      log.info(
          "mail.server.health status=UP host={} port={}",
          mailSender.getHost(),
          mailSender.getPort());
    } catch (MessagingException exception) {
      log.error(
          "mail.server.health status=DOWN host={} port={} message={}",
          mailSender.getHost(),
          mailSender.getPort(),
          exception.getMessage(),
          exception);
      throw new IllegalStateException("Mail server health check failed", exception);
    }
  }
}
