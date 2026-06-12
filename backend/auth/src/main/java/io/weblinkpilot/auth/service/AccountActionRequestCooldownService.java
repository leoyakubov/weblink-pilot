package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.exception.AccountActionRequestCooldownException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccountActionRequestCooldownService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final String REQUEST_COOLDOWN_KEY_PREFIX = "auth:action-request:";

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
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return ENCODER.encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash resend cooldown key", exception);
    }
  }
}
