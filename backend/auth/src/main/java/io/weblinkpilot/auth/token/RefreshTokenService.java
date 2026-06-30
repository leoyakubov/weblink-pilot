package io.weblinkpilot.auth.token;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.RefreshToken;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.repository.RefreshTokenRepository;
import io.weblinkpilot.auth.support.AfterCommitExecutor;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2")
public class RefreshTokenService {

  public record RotationResult(String username, String role, String refreshToken) {}

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final int TOKEN_RANDOM_BYTES = 32;

  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenSessionCache sessionCache;
  private final RefreshTokenUserIndex userIndex;
  private final AfterCommitExecutor afterCommitExecutor;
  private final Duration refreshTokenTtl;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenSessionCache sessionCache,
      RefreshTokenUserIndex userIndex,
      AfterCommitExecutor afterCommitExecutor,
      AuthProperties authProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.sessionCache = sessionCache;
    this.userIndex = userIndex;
    this.afterCommitExecutor = afterCommitExecutor;
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
          sessionCache.write(token.getTokenHash(), toSession(token));
          userIndex.add(token.getUser().getUsername(), token.getTokenHash());
        });
    return rawToken;
  }

  @Transactional
  public RotationResult rotateRefreshToken(String rawToken) {
    String tokenHash = hash(rawToken);
    RefreshTokenSessionCache.RefreshSession cachedSession = sessionCache.read(tokenHash);
    OffsetDateTime now = nowUtc();
    if (cachedSession != null && cachedSession.expiresAt().isBefore(now)) {
      sessionCache.evict(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    RefreshToken token =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

    if (!token.isActive(now)) {
      sessionCache.evict(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    if (!token.getUser().isEnabled()) {
      sessionCache.evict(tokenHash);
      throw new AccountDisabledException();
    }

    if (cachedSession != null && !token.getUser().getUsername().equals(cachedSession.username())) {
      sessionCache.evict(tokenHash);
      throw new InvalidRefreshTokenException();
    }

    token.revoke(now);
    refreshTokenRepository.save(token);
    afterCommit(
        () -> {
          sessionCache.evict(tokenHash);
          userIndex.remove(token.getUser().getUsername(), tokenHash);
        });

    String nextRawToken = generateToken();
    RefreshToken nextToken = persistRefreshToken(token.getUser(), nextRawToken);
    afterCommit(
        () -> {
          sessionCache.write(nextToken.getTokenHash(), toSession(nextToken));
          userIndex.add(nextToken.getUser().getUsername(), nextToken.getTokenHash());
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
    RefreshTokenSessionCache.RefreshSession cachedSession = sessionCache.read(tokenHash);
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
          sessionCache.evict(tokenHash);
          String username = usernameHolder[0];
          if (username == null && cachedSession != null) {
            username = cachedSession.username();
          }
          if (username != null) {
            userIndex.remove(username, tokenHash);
          }
        });
  }

  @Transactional
  public void revokeAllForUser(String username) {
    if (username == null || username.isBlank()) {
      return;
    }

    List<String> indexedTokenHashes = userIndex.readTokenHashes(username);
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
              indexedTokenHashes.forEach(sessionCache::evict);
              userIndex.delete(username);
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
          revokedTokens.forEach(token -> sessionCache.evict(token.getTokenHash()));
          userIndex.delete(resolvedUsername);
        });
  }

  private RefreshToken persistRefreshToken(UserAccount account, String rawToken) {
    OffsetDateTime issuedAt = nowUtc();
    RefreshToken token =
        new RefreshToken(hash(rawToken), account, issuedAt, issuedAt.plus(refreshTokenTtl));
    refreshTokenRepository.save(token);
    return token;
  }

  private RefreshTokenSessionCache.RefreshSession toSession(RefreshToken token) {
    return new RefreshTokenSessionCache.RefreshSession(
        token.getUser().getUsername(),
        token.getUser().getRoleName(),
        token.getCreatedAt(),
        token.getExpiresAt());
  }

  private void afterCommit(Runnable action) {
    afterCommitExecutor.execute(action);
  }

  private OffsetDateTime nowUtc() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
    secureRandom.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }

  private String hash(String token) {
    return TokenDigest.sha256Base64Url(token);
  }
}
