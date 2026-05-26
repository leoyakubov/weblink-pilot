package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.RefreshToken;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidRefreshTokenException;
import io.weblinkpilot.auth.repository.RefreshTokenRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository repository;

  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setRefreshTokenTtlDays(30);
    service = new RefreshTokenService(repository, authProperties);
  }

  @Test
  void issuesRefreshTokenWithFutureExpiry() {
    UserAccount account =
        new UserAccount("alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
    when(repository.save(tokenCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

    String rawToken = service.issueRefreshToken(account);

    assertThat(rawToken).isNotBlank();
    RefreshToken saved = tokenCaptor.getValue();
    assertThat(saved.getUser()).isSameAs(account);
    assertThat(saved.getExpiresAt()).isAfter(saved.getCreatedAt());
    assertThat(saved.getRevokedAt()).isNull();
  }

  @Test
  void rotatesActiveRefreshToken() {
    UserAccount account =
        new UserAccount("alice", "hashed", new Role("USER"), true, OffsetDateTime.now(ZoneOffset.UTC));
    RefreshToken activeToken =
        new RefreshToken(
            "existing-hash",
            account,
            OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
            OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(activeToken));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenService.RotationResult result = service.rotateRefreshToken("refresh-token");

    assertThat(result.account()).isSameAs(account);
    assertThat(result.refreshToken()).isNotBlank();
    assertThat(activeToken.getRevokedAt()).isNotNull();
  }

  @Test
  void rejectsMissingRefreshToken() {
    when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotateRefreshToken("missing"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void validateConfigurationRejectsNonPositiveTtl() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setRefreshTokenTtlDays(0);
    RefreshTokenService invalidService = new RefreshTokenService(repository, authProperties);

    assertThatThrownBy(invalidService::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Refresh token TTL must be positive");
  }
}
