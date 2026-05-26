package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.RefreshToken;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.repository.RefreshTokenRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  public record RotationResult(UserAccount account, String refreshToken) {}

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final RefreshTokenRepository repository;
  private final Duration refreshTokenTtl;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(RefreshTokenRepository repository, AuthProperties authProperties) {
    this.repository = repository;
    this.refreshTokenTtl = Duration.ofDays(authProperties.getRefreshTokenTtlDays());
  }

  @PostConstruct
  void validateConfiguration() {
    if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
      throw new IllegalStateException("Refresh token TTL must be positive");
    }
  }

  @Transactional
  public String issueRefreshToken(UserAccount account) {
    OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
    String rawToken = generateToken();
    repository.save(
        new RefreshToken(
            hash(rawToken), account, issuedAt, issuedAt.plus(refreshTokenTtl)));
    return rawToken;
  }

  @Transactional
  public RotationResult rotateRefreshToken(String rawToken) {
    RefreshToken token =
        repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (!token.isActive(now)) {
      throw new InvalidRefreshTokenException();
    }
    if (!token.getUser().isEnabled()) {
      throw new AccountDisabledException(token.getUser().getUsername());
    }

    token.revoke(now);
    OffsetDateTime issuedAt = now;
    String nextRawToken = generateToken();
    repository.save(token);
    repository.save(
        new RefreshToken(
            hash(nextRawToken), token.getUser(), issuedAt, issuedAt.plus(refreshTokenTtl)));
    return new RotationResult(token.getUser(), nextRawToken);
  }

  @Transactional
  public void revokeRefreshToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }

    repository
        .findByTokenHash(hash(rawToken))
        .filter(token -> token.getRevokedAt() == null)
        .ifPresent(
            token -> {
              token.revoke(OffsetDateTime.now(ZoneOffset.UTC));
              repository.save(token);
            });
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return ENCODER.encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash refresh token", exception);
    }
  }
}
