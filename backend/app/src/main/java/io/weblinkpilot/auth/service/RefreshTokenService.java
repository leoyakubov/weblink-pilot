package io.weblinkpilot.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RefreshTokenService {

  public record RotationResult(UserAccount account, String refreshToken) {}

  public record RefreshSession(
      String username, String role, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {}

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final RefreshTokenRepository refreshTokenRepository;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final Duration refreshTokenTtl;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      AuthProperties authProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper.copy().findAndRegisterModules();
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
    String rawToken = generateToken();
    RefreshToken token = persistRefreshToken(account, rawToken);
    afterCommit(() -> cacheSession(token.getTokenHash(), toSession(token)));
    return rawToken;
  }

  @Transactional
  public RotationResult rotateRefreshToken(String rawToken) {
    String tokenHash = hash(rawToken);
    RefreshToken token =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

    OffsetDateTime now = nowUtc();
    if (!token.isActive(now)) {
      evictSession(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    if (!token.getUser().isEnabled()) {
      evictSession(tokenHash);
      throw new AccountDisabledException(token.getUser().getUsername());
    }

    RefreshSession cachedSession = readSession(tokenHash);
    if (cachedSession != null
        && !token.getUser().getUsername().equals(cachedSession.username())) {
      evictSession(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    token.revoke(now);
    refreshTokenRepository.save(token);
    afterCommit(() -> evictSession(tokenHash));

    String nextRawToken = generateToken();
    RefreshToken nextToken = persistRefreshToken(token.getUser(), nextRawToken);
    afterCommit(() -> cacheSession(nextToken.getTokenHash(), toSession(nextToken)));
    return new RotationResult(token.getUser(), nextRawToken);
  }

  @Transactional
  public void revokeRefreshToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }

    String tokenHash = hash(rawToken);
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .filter(token -> token.getRevokedAt() == null)
        .ifPresent(
            token -> {
              token.revoke(nowUtc());
              refreshTokenRepository.save(token);
            });
    afterCommit(() -> evictSession(tokenHash));
  }

  private RefreshToken persistRefreshToken(UserAccount account, String rawToken) {
    OffsetDateTime issuedAt = nowUtc();
    RefreshToken token =
        new RefreshToken(
            hash(rawToken), account, issuedAt, issuedAt.plus(refreshTokenTtl));
    refreshTokenRepository.save(token);
    return token;
  }

  private RefreshSession toSession(RefreshToken token) {
    return new RefreshSession(
        token.getUser().getUsername(),
        token.getUser().getRoleName(),
        token.getCreatedAt(),
        token.getExpiresAt());
  }

  private RefreshSession readSession(String tokenHash) {
    try {
      String json = redisTemplate.opsForValue().get(redisKey(tokenHash));
      if (json == null || json.isBlank()) {
        return null;
      }
      return objectMapper.readValue(json, RefreshSession.class);
    } catch (Exception exception) {
      return null;
    }
  }

  private void cacheSession(String tokenHash, RefreshSession session) {
    try {
      redisTemplate
          .opsForValue()
          .set(redisKey(tokenHash), objectMapper.writeValueAsString(session), refreshTokenTtl);
    } catch (Exception exception) {
      // Cache is best-effort; the database remains the source of truth.
    }
  }

  private void evictSession(String tokenHash) {
    try {
      redisTemplate.delete(redisKey(tokenHash));
    } catch (Exception exception) {
      // Best-effort cache eviction only.
    }
  }

  private void afterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }

  private OffsetDateTime nowUtc() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }

  private String redisKey(String tokenHash) {
    return "auth:refresh:" + tokenHash;
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
