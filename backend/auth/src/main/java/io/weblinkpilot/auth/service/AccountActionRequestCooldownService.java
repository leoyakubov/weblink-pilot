package io.weblinkpilot.auth.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.exception.AccountActionRequestCooldownException;
import io.weblinkpilot.auth.token.TokenDigest;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this service.")
public class AccountActionRequestCooldownService {

  private static final String REQUEST_COOLDOWN_KEY_PREFIX = "auth:action-request:";
  private static final Logger log =
      LoggerFactory.getLogger(AccountActionRequestCooldownService.class);

  private final StringRedisTemplate redisTemplate;
  private final Duration cooldown;

  public AccountActionRequestCooldownService(
      StringRedisTemplate redisTemplate, AuthProperties authProperties) {
    this.redisTemplate = redisTemplate;
    this.cooldown = Duration.ofSeconds(authProperties.getAccountActionRequestCooldownSeconds());
  }

  public void enforceCooldown(String action, String email) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail.isBlank()) {
      return;
    }

    try {
      Boolean acquired =
          redisTemplate
              .opsForValue()
              .setIfAbsent(cooldownKey(action, normalizedEmail), "1", cooldown);
      if (Boolean.FALSE.equals(acquired)) {
        log.warn(
            "auth.action-request.cooldown.rejected action={} retryAfterSeconds={}",
            normalizeAction(action),
            cooldown.getSeconds());
        throw new AccountActionRequestCooldownException(action, cooldown.getSeconds());
      }
    } catch (AccountActionRequestCooldownException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      // Best effort only. The request should still work if Redis is temporarily unavailable.
    }
  }

  private String cooldownKey(String action, String email) {
    return REQUEST_COOLDOWN_KEY_PREFIX + normalizeAction(action) + ":" + hash(email);
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
  }

  private String normalizeAction(String action) {
    return action == null || action.isBlank()
        ? "unknown"
        : action.trim().toLowerCase(java.util.Locale.ROOT);
  }

  private String hash(String value) {
    return TokenDigest.sha256Base64Url(value);
  }
}
