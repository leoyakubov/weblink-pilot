package io.weblinkpilot.config.rate;

public record RateLimitDecision(
    boolean allowed, long limit, long remaining, long retryAfterSeconds) {
  public static RateLimitDecision allowed(long limit, long remaining) {
    return new RateLimitDecision(true, limit, remaining, 0L);
  }

  public static RateLimitDecision blocked(long limit, long remaining, long retryAfterSeconds) {
    return new RateLimitDecision(false, limit, remaining, retryAfterSeconds);
  }
}
