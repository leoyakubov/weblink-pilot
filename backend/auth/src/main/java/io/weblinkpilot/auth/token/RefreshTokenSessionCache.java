package io.weblinkpilot.auth.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed Redis and JSON dependencies are intentionally retained.")
public class RefreshTokenSessionCache {

  public record RefreshSession(
      String username, String role, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {}

  private static final String KEY_PREFIX = "auth:refresh:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  public RefreshTokenSessionCache(
      StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AuthProperties authProperties) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper.copy().findAndRegisterModules();
    this.ttl = Duration.ofDays(authProperties.getRefreshTokenTtlDays());
  }

  public RefreshSession read(String tokenHash) {
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

  public void write(String tokenHash, RefreshSession session) {
    try {
      redisTemplate
          .opsForValue()
          .set(redisKey(tokenHash), objectMapper.writeValueAsString(session), ttl);
    } catch (java.io.IOException | RuntimeException exception) {
      // Cache is best-effort; PostgreSQL remains the source of truth.
    }
  }

  public void evict(String tokenHash) {
    try {
      redisTemplate.delete(redisKey(tokenHash));
    } catch (RuntimeException exception) {
      // Best-effort cache eviction only.
    }
  }

  private String redisKey(String tokenHash) {
    return KEY_PREFIX + tokenHash;
  }
}
