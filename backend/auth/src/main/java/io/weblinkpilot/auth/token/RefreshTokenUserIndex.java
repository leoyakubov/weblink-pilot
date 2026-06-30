package io.weblinkpilot.auth.token;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed Redis dependency is intentionally retained.")
public class RefreshTokenUserIndex {

  private static final String KEY_PREFIX = "auth:refresh:user:";

  private final StringRedisTemplate redisTemplate;

  public RefreshTokenUserIndex(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void add(String username, String tokenHash) {
    try {
      redisTemplate.opsForSet().add(redisKey(username), tokenHash);
    } catch (RuntimeException exception) {
      // Best-effort index maintenance only.
    }
  }

  public void remove(String username, String tokenHash) {
    try {
      redisTemplate.opsForSet().remove(redisKey(username), tokenHash);
    } catch (RuntimeException exception) {
      // Best-effort index maintenance only.
    }
  }

  public List<String> readTokenHashes(String username) {
    try {
      Set<String> tokenHashes = redisTemplate.opsForSet().members(redisKey(username));
      if (tokenHashes == null || tokenHashes.isEmpty()) {
        return List.of();
      }
      return List.copyOf(tokenHashes);
    } catch (RuntimeException exception) {
      return List.of();
    }
  }

  public void delete(String username) {
    try {
      redisTemplate.delete(redisKey(username));
    } catch (RuntimeException exception) {
      // Best-effort cache eviction only.
    }
  }

  private String redisKey(String username) {
    return KEY_PREFIX + username;
  }
}
