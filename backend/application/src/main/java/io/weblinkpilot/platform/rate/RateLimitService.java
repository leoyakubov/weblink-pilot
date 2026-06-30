package io.weblinkpilot.platform.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.weblinkpilot.platform.web.ApiRoutes;
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

    RateLimitPolicy policy = policyFor(path);
    if (policy == null) {
      return RateLimitDecision.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    Bucket bucket =
        buckets.computeIfAbsent(policy.name() + ":" + clientIp, key -> bucketFor(policy));
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return RateLimitDecision.allowed(limitFor(policy), probe.getRemainingTokens());
    }

    long retryAfterSeconds =
        Math.max(1L, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
    return RateLimitDecision.blocked(limitFor(policy), 0L, retryAfterSeconds);
  }

  private Bucket bucketFor(RateLimitPolicy policy) {
    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(limitFor(policy))
                .refillGreedy(limitFor(policy), Duration.ofMinutes(1))
                .build())
        .build();
  }

  private int limitFor(RateLimitPolicy policy) {
    return switch (policy) {
      case AUTH -> authPerMinute;
      case REDIRECT -> redirectPerMinute;
      case API -> apiPerMinute;
    };
  }

  private RateLimitPolicy policyFor(String path) {
    if (path == null) {
      return null;
    }
    if (path.startsWith(ApiRoutes.REDIRECT_PREFIX)) {
      return RateLimitPolicy.REDIRECT;
    }
    if (path.startsWith(ApiRoutes.QR_REDIRECT_PREFIX)) {
      return RateLimitPolicy.REDIRECT;
    }
    if (isPublicAuthPath(path)) {
      return RateLimitPolicy.AUTH;
    }
    if (path.startsWith(ApiRoutes.API_V1_PREFIX)) {
      return RateLimitPolicy.API;
    }
    return null;
  }

  private boolean isPublicAuthPath(String path) {
    return ApiRoutes.PUBLIC_AUTH_PATHS.contains(path)
        || path.startsWith(ApiRoutes.AUTH_GITHUB_OAUTH_PREFIX);
  }
}
