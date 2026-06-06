package io.weblinkpilot.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2")
public class RefreshTokenService {

  public record RotationResult(String username, String role, String refreshToken) {}

  public record RefreshSession(
      String username, String role, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {}

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final String REFRESH_SESSION_KEY_PREFIX = "auth:refresh:";
  private static final String REFRESH_USER_INDEX_KEY_PREFIX = "auth:refresh:user:";

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
    afterCommit(
        () -> {
          cacheSession(token.getTokenHash(), toSession(token));
          indexTokenForUser(token.getUser().getUsername(), token.getTokenHash());
        });
    return rawToken;
  }

  @Transactional
  public RotationResult rotateRefreshToken(String rawToken) {
    String tokenHash = hash(rawToken);
    RefreshSession cachedSession = readSession(tokenHash);
    OffsetDateTime now = nowUtc();
    if (cachedSession != null && cachedSession.expiresAt().isBefore(now)) {
      evictSession(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    RefreshToken token =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

    if (!token.isActive(now)) {
      evictSession(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    if (!token.getUser().isEnabled()) {
      evictSession(tokenHash);
      throw new AccountDisabledException();
    }

    if (cachedSession != null && !token.getUser().getUsername().equals(cachedSession.username())) {
      evictSession(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    token.revoke(now);
    refreshTokenRepository.save(token);
    afterCommit(
        () -> {
          evictSession(tokenHash);
          unindexTokenForUser(token.getUser().getUsername(), tokenHash);
        });

    String nextRawToken = generateToken();
    RefreshToken nextToken = persistRefreshToken(token.getUser(), nextRawToken);
    afterCommit(
        () -> {
          cacheSession(nextToken.getTokenHash(), toSession(nextToken));
          indexTokenForUser(nextToken.getUser().getUsername(), nextToken.getTokenHash());
        });
    return new RotationResult(
        token.getUser().getUsername(), token.getUser().getRoleName(), nextRawToken);
  }

  @Transactional
  public void revokeRefreshToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return;
    }

    String tokenHash = hash(rawToken);
    RefreshSession cachedSession = readSession(tokenHash);
    final String[] usernameHolder = new String[1];
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .ifPresent(
            token -> {
              usernameHolder[0] = token.getUser().getUsername();
              if (token.getRevokedAt() == null) {
                token.revoke(nowUtc());
                refreshTokenRepository.save(token);
              }
            });
    afterCommit(
        () -> {
          evictSession(tokenHash);
          String username = usernameHolder[0];
          if (username == null && cachedSession != null) {
            username = cachedSession.username();
          }
          if (username != null) {
            unindexTokenForUser(username, tokenHash);
          }
        });
  }

  @Transactional
  public void revokeAllForUser(String username) {
    if (username == null || username.isBlank()) {
      return;
    }

    List<String> indexedTokenHashes = readIndexedTokenHashes(username);
    List<RefreshToken> tokens =
        indexedTokenHashes.isEmpty()
            ? refreshTokenRepository.findAllByUsername(username)
            : refreshTokenRepository.findAllByTokenHashIn(indexedTokenHashes);
    if (tokens.isEmpty() && !indexedTokenHashes.isEmpty()) {
      tokens = refreshTokenRepository.findAllByUsername(username);
    }
    if (tokens.isEmpty()) {
      if (!indexedTokenHashes.isEmpty()) {
        afterCommit(
            () -> {
              indexedTokenHashes.forEach(this::evictSession);
              deleteUserTokenIndex(username);
            });
      }
      return;
    }

    OffsetDateTime now = nowUtc();
    tokens.stream()
        .filter(token -> token.getRevokedAt() == null)
        .forEach(token -> token.revoke(now));
    refreshTokenRepository.saveAll(tokens);
    String resolvedUsername = username;
    final List<RefreshToken> revokedTokens = tokens;
    afterCommit(
        () -> {
          revokedTokens.forEach(token -> evictSession(token.getTokenHash()));
          deleteUserTokenIndex(resolvedUsername);
        });
  }

  private RefreshToken persistRefreshToken(UserAccount account, String rawToken) {
    OffsetDateTime issuedAt = nowUtc();
    RefreshToken token =
        new RefreshToken(hash(rawToken), account, issuedAt, issuedAt.plus(refreshTokenTtl));
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
    } catch (java.io.IOException | RuntimeException exception) {
      return null;
    }
  }

  private void cacheSession(String tokenHash, RefreshSession session) {
    try {
      redisTemplate
          .opsForValue()
          .set(
              sessionRedisKey(tokenHash),
              objectMapper.writeValueAsString(session),
              refreshTokenTtl);
    } catch (java.io.IOException | RuntimeException exception) {
      // Cache is best-effort; the database remains the source of truth.
    }
  }

  private void evictSession(String tokenHash) {
    try {
      redisTemplate.delete(sessionRedisKey(tokenHash));
    } catch (RuntimeException exception) {
      // Best-effort cache eviction only.
    }
  }

  private void indexTokenForUser(String username, String tokenHash) {
    try {
      redisTemplate.opsForSet().add(userIndexRedisKey(username), tokenHash);
    } catch (RuntimeException exception) {
      // Best-effort index maintenance only.
    }
  }

  private void unindexTokenForUser(String username, String tokenHash) {
    try {
      redisTemplate.opsForSet().remove(userIndexRedisKey(username), tokenHash);
    } catch (RuntimeException exception) {
      // Best-effort index maintenance only.
    }
  }

  private List<String> readIndexedTokenHashes(String username) {
    try {
      Set<String> tokenHashes = redisTemplate.opsForSet().members(userIndexRedisKey(username));
      if (tokenHashes == null || tokenHashes.isEmpty()) {
        return List.of();
      }
      return List.copyOf(tokenHashes);
    } catch (RuntimeException exception) {
      return List.of();
    }
  }

  private void deleteUserTokenIndex(String username) {
    try {
      redisTemplate.delete(userIndexRedisKey(username));
    } catch (RuntimeException exception) {
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
    return sessionRedisKey(tokenHash);
  }

  private String sessionRedisKey(String tokenHash) {
    return REFRESH_SESSION_KEY_PREFIX + tokenHash;
  }

  private String userIndexRedisKey(String username) {
    return REFRESH_USER_INDEX_KEY_PREFIX + username;
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
