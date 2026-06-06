package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

  private static final String RESET_PATH = "/auth/reset-password";

  private final UserAccountRepository userAccountRepository;
  private final UserAccountService userAccountService;
  private final PasswordEncoder passwordEncoder;
  private final AccountActionTokenService tokenService;
  private final ApplicationEventPublisher eventPublisher;
  private final String frontendBaseUrl;

  public PasswordResetService(
      UserAccountRepository userAccountRepository,
      UserAccountService userAccountService,
      PasswordEncoder passwordEncoder,
      AccountActionTokenService tokenService,
      ApplicationEventPublisher eventPublisher,
      AuthProperties authProperties) {
    this.userAccountRepository = userAccountRepository;
    this.userAccountService = userAccountService;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.eventPublisher = eventPublisher;
    this.frontendBaseUrl = authProperties.getFrontendBaseUrl();
  }

  @Transactional
  public void requestPasswordReset(String email) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail.isBlank()) {
      throw new IllegalArgumentException("Email is required.");
    }

    userAccountRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(this::sendResetLink);
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
  }

  private void sendResetLink(UserAccount account) {
    String token = tokenService.issueToken(account, AccountActionTokenType.PASSWORD_RESET);
    String link =
        frontendBaseUrl + RESET_PATH + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    eventPublisher.publishEvent(new PasswordResetLinkRequestedEvent(account.getEmail(), link));
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
