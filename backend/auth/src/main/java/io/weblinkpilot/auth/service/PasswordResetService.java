package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

  private static final String RESET_PATH = "/auth/reset-password";
  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

  private final UserAccountRepository userAccountRepository;
  private final UserAccountService userAccountService;
  private final PasswordEncoder passwordEncoder;
  private final AccountActionTokenService tokenService;
  private final AccountActionRequestCooldownService cooldownService;
  private final ApplicationEventPublisher eventPublisher;
  private final String frontendBaseUrl;
  private final boolean demoMailboxEnabled;

  public PasswordResetService(
      UserAccountRepository userAccountRepository,
      UserAccountService userAccountService,
      PasswordEncoder passwordEncoder,
      AccountActionTokenService tokenService,
      AccountActionRequestCooldownService cooldownService,
      ApplicationEventPublisher eventPublisher,
      AuthProperties authProperties) {
    this.userAccountRepository = userAccountRepository;
    this.userAccountService = userAccountService;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.cooldownService = cooldownService;
    this.eventPublisher = eventPublisher;
    this.frontendBaseUrl = authProperties.getFrontendBaseUrl();
    this.demoMailboxEnabled = authProperties.isDemoMailboxEnabled();
  }

  @Transactional
  public String requestPasswordReset(String email) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail.isBlank()) {
      throw new IllegalArgumentException("Email is required.");
    }

    log.debug("auth.password-reset.requested email={}", normalizedEmail);

    UserAccount account = userAccountRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
    if (account == null) {
      log.debug("auth.password-reset.skipped reason=account_not_found email={}", normalizedEmail);
      return null;
    }

    cooldownService.enforceCooldown("password reset", account.getEmail());
    return sendResetLink(account);
  }

  @Transactional
  public void confirmPasswordReset(String rawToken, String newPassword) {
    userAccountService.validatePasswordPolicy(newPassword);
    UserAccount account =
        tokenService
            .consumeToken(rawToken, AccountActionTokenType.PASSWORD_RESET)
            .orElseThrow(InvalidAccountActionTokenException::new)
            .getUser();
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    userAccountRepository.save(account);
    log.info(
        "auth.password-reset.confirmed username={} email={}",
        account.getUsername(),
        account.getEmail());
  }

  private String sendResetLink(UserAccount account) {
    String token = tokenService.issueToken(account, AccountActionTokenType.PASSWORD_RESET);
    String link =
        frontendBaseUrl + RESET_PATH + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    log.debug(
        "auth.password-reset.queued username={} email={}",
        account.getUsername(),
        account.getEmail());
    if (demoMailboxEnabled) {
      log.info(
          "auth.password-reset.demo-preview username={} email={} link={}",
          account.getUsername(),
          account.getEmail(),
          link);
      return link;
    }

    eventPublisher.publishEvent(new PasswordResetLinkRequestedEvent(account.getEmail(), link));
    return link;
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
