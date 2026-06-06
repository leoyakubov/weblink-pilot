package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

  private static final String VERIFY_PATH = "/auth/verify-email";

  private final UserAccountRepository userAccountRepository;
  private final AccountActionTokenService tokenService;
  private final ApplicationEventPublisher eventPublisher;
  private final String frontendBaseUrl;

  public EmailVerificationService(
      UserAccountRepository userAccountRepository,
      AccountActionTokenService tokenService,
      ApplicationEventPublisher eventPublisher,
      AuthProperties authProperties) {
    this.userAccountRepository = userAccountRepository;
    this.tokenService = tokenService;
    this.eventPublisher = eventPublisher;
    this.frontendBaseUrl = authProperties.getFrontendBaseUrl();
  }

  @Transactional
  public void requestEmailVerification(String email) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail.isBlank()) {
      throw new IllegalArgumentException("Email is required.");
    }

    userAccountRepository
        .findByEmailIgnoreCase(normalizedEmail)
        .filter(account -> !account.isEmailVerified())
        .ifPresent(this::sendVerificationLink);
  }

  @Transactional
  public void confirmEmailVerification(String rawToken) {
    UserAccount account =
        tokenService
            .consumeToken(rawToken, AccountActionTokenType.EMAIL_VERIFICATION)
            .orElseThrow(InvalidAccountActionTokenException::new)
            .getUser();
    account.markEmailVerified(nowUtc());
    userAccountRepository.save(account);
  }

  private void sendVerificationLink(UserAccount account) {
    String token = tokenService.issueToken(account, AccountActionTokenType.EMAIL_VERIFICATION);
    String link =
        frontendBaseUrl
            + VERIFY_PATH
            + "?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
    eventPublisher.publishEvent(new EmailVerificationLinkRequestedEvent(account.getEmail(), link));
  }

  private OffsetDateTime nowUtc() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
