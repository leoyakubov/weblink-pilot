package io.weblinkpilot.config.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

  private final boolean enabled;
  private final int apiPerMinute;
  private final int redirectPerMinute;
  private final int authPerMinute;
  private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public RateLimitService(RateLimitProperties properties) {
    this.enabled = properties.isEnabled();
    this.apiPerMinute = properties.getApiPerMinute();
    this.redirectPerMinute = properties.getRedirectPerMinute();
    this.authPerMinute = properties.getAuthPerMinute();
  }

  public RateLimitDecision tryConsume(String path, String clientIp) {
    if (!enabled) {
      return RateLimitDecision.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    String policy = policyFor(path);
    if (policy == null) {
      return RateLimitDecision.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    Bucket bucket = buckets.computeIfAbsent(policy + ":" + clientIp, key -> bucketFor(policy));
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return RateLimitDecision.allowed(limitFor(policy), probe.getRemainingTokens());
    }

    long retryAfterSeconds =
        Math.max(1L, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
    return RateLimitDecision.blocked(limitFor(policy), 0L, retryAfterSeconds);
  }

  private Bucket bucketFor(String policy) {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(limitFor(policy))
                .refillGreedy(limitFor(policy), Duration.ofMinutes(1))
                .build())
        .build();
  }

  private int limitFor(String policy) {
    return switch (policy) {
      case "auth" -> authPerMinute;
      case "redirect" -> redirectPerMinute;
      default -> apiPerMinute;
    };
  }

  private String policyFor(String path) {
    if (path == null) {
      return null;
    }
    if (path.startsWith("/r/")) {
      return "redirect";
    }
    if (path.startsWith("/q/")) {
      return "redirect";
    }
    if (isPublicAuthPath(path)) {
      return "auth";
    }
    if (path.startsWith("/api/v1/")) {
      return "api";
    }
    return null;
  }

  private boolean isPublicAuthPath(String path) {
    return path.equals("/api/v1/auth/register")
        || path.equals("/api/v1/auth/login")
        || path.equals("/api/v1/auth/refresh")
        || path.equals("/api/v1/auth/logout")
        || path.equals("/api/v1/auth/password-reset/request")
        || path.equals("/api/v1/auth/password-reset/confirm")
        || path.equals("/api/v1/auth/email-verification/request")
        || path.equals("/api/v1/auth/email-verification/confirm")
        || path.startsWith("/api/v1/auth/oauth2/github/");
  }
}
