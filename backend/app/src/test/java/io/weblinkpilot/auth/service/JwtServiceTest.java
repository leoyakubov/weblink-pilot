package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.weblinkpilot.auth.config.AuthProperties;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private AuthProperties authProperties;
  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    authProperties = new AuthProperties();
    authProperties.setIssuer("weblinkpilot");
    authProperties.setJwtSecret("super-secret");
    authProperties.setTokenTtlMinutes(15);
    jwtService = new JwtService(new ObjectMapper(), authProperties);
  }

  @Test
  void issuesAndParsesToken() {
    String token = jwtService.issueToken("alice", "USER");

    var claims = jwtService.parseToken(token);

    assertThat(claims).isPresent();
    assertThat(claims.orElseThrow().username()).isEqualTo("alice");
    assertThat(claims.orElseThrow().role()).isEqualTo("USER");
    assertThat(claims.orElseThrow().issuedAt()).isBeforeOrEqualTo(Instant.now());
    assertThat(claims.orElseThrow().expiresAt()).isAfter(claims.orElseThrow().issuedAt());
  }

  @Test
  void parseTokenReturnsEmptyForMalformedToken() {
    assertThat(jwtService.parseToken("not-a-jwt")).isEmpty();
  }

  @Test
  void parseTokenReturnsEmptyForBadSignature() {
    String token = jwtService.issueToken("alice", "USER");
    String[] parts = token.split("\\.");
    String tampered = parts[0] + "." + parts[1] + "A" + "." + parts[2];

    assertThat(jwtService.parseToken(tampered)).isEmpty();
  }

  @Test
  void validateConfigurationRejectsBlankSecret() {
    authProperties.setJwtSecret(" ");
    JwtService invalidService = new JwtService(new ObjectMapper(), authProperties);

    assertThatThrownBy(invalidService::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT secret must be configured");
  }

  @Test
  void validateConfigurationRejectsShortSecret() {
    authProperties.setJwtSecret("too-short-secret");
    JwtService invalidService = new JwtService(new ObjectMapper(), authProperties);

    assertThatThrownBy(invalidService::validateConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 characters");
  }
}
