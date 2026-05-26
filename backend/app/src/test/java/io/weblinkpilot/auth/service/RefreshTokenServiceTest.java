package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.RefreshToken;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.repository.RefreshTokenRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private RefreshTokenService service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setRefreshTokenTtlDays(30);
    objectMapper = new ObjectMapper().findAndRegisterModules();
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    lenient()
        .when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    service =
        new RefreshTokenService(
            refreshTokenRepository, redisTemplate, objectMapper, authProperties);
  }

  @Test
  void issuesRefreshTokenWithFutureExpiry() throws Exception {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

    String rawToken = service.issueRefreshToken(account);

    assertThat(rawToken).isNotBlank();
    verify(refreshTokenRepository).save(tokenCaptor.capture());
    RefreshToken savedToken = tokenCaptor.getValue();
    assertThat(savedToken.getUser()).isSameAs(account);
    assertThat(savedToken.getExpiresAt()).isAfter(savedToken.getCreatedAt());

    verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
    assertThat(keyCaptor.getValue()).startsWith("auth:refresh:");
    assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(30));

    RefreshTokenService.RefreshSession session =
        objectMapper.readValue(valueCaptor.getValue(), RefreshTokenService.RefreshSession.class);
    assertThat(session.username()).isEqualTo("alice");
    assertThat(session.role()).isEqualTo("USER");
    assertThat(session.expiresAt()).isAfter(session.issuedAt());
  }

  @Test
  void rotatesActiveRefreshTokenAndUsesCacheIfAvailable() throws Exception {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
    RefreshToken token = new RefreshToken("token-hash", account, issuedAt, issuedAt.plusDays(30));
    RefreshTokenService.RefreshSession session =
        new RefreshTokenService.RefreshSession("alice", "USER", issuedAt, issuedAt.plusDays(30));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    when(valueOperations.get(anyString())).thenReturn(objectMapper.writeValueAsString(session));

    RefreshTokenService.RotationResult result = service.rotateRefreshToken("refresh-token");

    assertThat(result.username()).isEqualTo("alice");
    assertThat(result.role()).isEqualTo("USER");
    assertThat(result.refreshToken()).isNotBlank();
    verify(valueOperations).get(anyString());
    verify(refreshTokenRepository).save(token);
    verify(redisTemplate).delete(anyString());
    verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void rejectsMissingRefreshToken() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotateRefreshToken("missing"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void rejectsExpiredRefreshToken() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(31);
    RefreshToken token = new RefreshToken("token-hash", account, issuedAt, issuedAt.plusDays(30));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> service.rotateRefreshToken("expired"))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(redisTemplate).delete(anyString());
  }

  @Test
  void revokeRefreshTokenDeletesRedisKey() {
    UserAccount account =
        new UserAccount(
            "alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
    RefreshToken token = new RefreshToken("token-hash", account, issuedAt, issuedAt.plusDays(30));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    service.revokeRefreshToken("refresh-token");

    verify(refreshTokenRepository).save(token);
    assertThat(token.getRevokedAt()).isNotNull();
    verify(redisTemplate).delete(anyString());
  }
}
