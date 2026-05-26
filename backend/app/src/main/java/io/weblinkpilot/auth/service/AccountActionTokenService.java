package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionToken;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.repository.AccountActionTokenRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountActionTokenService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final AccountActionTokenRepository repository;
  private final long tokenTtlHours;

  public AccountActionTokenService(
      AccountActionTokenRepository repository, AuthProperties authProperties) {
    this.repository = repository;
    this.tokenTtlHours = authProperties.getAccountActionTokenTtlHours();
  }

  @PostConstruct
  void validateConfiguration() {
    if (tokenTtlHours <= 0L) {
      throw new IllegalStateException("Account action token TTL must be positive");
    }
  }

  @Transactional
  public String issueToken(UserAccount user, AccountActionTokenType type) {
    String rawToken = generateToken();
    OffsetDateTime issuedAt = nowUtc();
    AccountActionToken token =
        new AccountActionToken(
            hash(rawToken), type, user, issuedAt, issuedAt.plusHours(tokenTtlHours));
    repository.save(token);
    return rawToken;
  }

  @Transactional
  public Optional<AccountActionToken> consumeToken(String rawToken, AccountActionTokenType type) {
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }

    String tokenHash = hash(rawToken);
    AccountActionToken token =
        repository
            .findByTokenHashAndType(tokenHash, type)
            .orElseThrow(InvalidAccountActionTokenException::new);

    if (!token.isActive(nowUtc())) {
      throw new InvalidAccountActionTokenException();
    }

    token.consume(nowUtc());
    repository.save(token);
    return Optional.of(token);
  }

  private OffsetDateTime nowUtc() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  private String generateToken() {
    return ENCODER.encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return ENCODER.encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash account action token", exception);
    }
  }
}
