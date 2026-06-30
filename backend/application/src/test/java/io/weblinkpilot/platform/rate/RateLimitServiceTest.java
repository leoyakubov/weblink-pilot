package io.weblinkpilot.platform.rate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

  @Test
  void disabledPolicyAllowsAnyRequest() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(false);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision decision = service.tryConsume("/api/v1/urls", "127.0.0.1");

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.limit()).isEqualTo(Long.MAX_VALUE);
    assertThat(decision.remaining()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void unknownPathIsNotRateLimited() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(true);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision decision = service.tryConsume("/health", "127.0.0.1");

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.limit()).isEqualTo(Long.MAX_VALUE);
    assertThat(decision.remaining()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void apiPolicyUsesApiLimit() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(true);
    properties.setApiPerMinute(1);
    properties.setRedirectPerMinute(1);
    properties.setAuthPerMinute(1);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision first = service.tryConsume("/api/v1/urls/demo", "127.0.0.1");
    RateLimitDecision second = service.tryConsume("/api/v1/urls/demo", "127.0.0.1");

    assertThat(first.allowed()).isTrue();
    assertThat(first.limit()).isEqualTo(1L);
    assertThat(first.remaining()).isEqualTo(0L);
    assertThat(second.allowed()).isFalse();
    assertThat(second.limit()).isEqualTo(1L);
    assertThat(second.retryAfterSeconds()).isGreaterThanOrEqualTo(1L);
  }

  @Test
  void redirectPolicyUsesRedirectLimitForShortPath() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(true);
    properties.setApiPerMinute(10);
    properties.setRedirectPerMinute(1);
    properties.setAuthPerMinute(10);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision first = service.tryConsume("/r/demo", "127.0.0.2");
    RateLimitDecision second = service.tryConsume("/r/demo", "127.0.0.2");

    assertThat(first.allowed()).isTrue();
    assertThat(first.limit()).isEqualTo(1L);
    assertThat(second.allowed()).isFalse();
  }

  @Test
  void redirectPolicyAlsoAppliesToQrPath() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(true);
    properties.setApiPerMinute(10);
    properties.setRedirectPerMinute(1);
    properties.setAuthPerMinute(10);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision first = service.tryConsume("/q/demo", "127.0.0.3");
    RateLimitDecision second = service.tryConsume("/q/demo", "127.0.0.3");

    assertThat(first.allowed()).isTrue();
    assertThat(first.limit()).isEqualTo(1L);
    assertThat(second.allowed()).isFalse();
  }

  @Test
  void authPolicyUsesDedicatedAuthLimit() {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(true);
    properties.setApiPerMinute(10);
    properties.setRedirectPerMinute(10);
    properties.setAuthPerMinute(1);
    RateLimitService service = new RateLimitService(properties);

    RateLimitDecision first = service.tryConsume("/api/v1/auth/login", "127.0.0.4");
    RateLimitDecision second =
        service.tryConsume("/api/v1/auth/password-reset/request", "127.0.0.4");

    assertThat(first.allowed()).isTrue();
    assertThat(first.limit()).isEqualTo(1L);
    assertThat(second.allowed()).isFalse();
    assertThat(second.limit()).isEqualTo(1L);
  }
}
