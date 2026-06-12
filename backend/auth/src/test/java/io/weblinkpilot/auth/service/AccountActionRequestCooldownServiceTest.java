package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.exception.AccountActionRequestCooldownException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AccountActionRequestCooldownServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private AccountActionRequestCooldownService service;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setAccountActionRequestCooldownSeconds(30);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service = new AccountActionRequestCooldownService(redisTemplate, authProperties);
  }

  @Test
  void allowsFirstRequestAndSetsCooldownKey() {
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);

    service.enforceCooldown("password reset", "alice@example.com");

    verify(valueOperations).setIfAbsent(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void rejectsRepeatedRequestDuringCooldown() {
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    assertThatThrownBy(() -> service.enforceCooldown("verification email", "alice@example.com"))
        .isInstanceOf(AccountActionRequestCooldownException.class)
        .hasMessageContaining("30 seconds");
  }
}
