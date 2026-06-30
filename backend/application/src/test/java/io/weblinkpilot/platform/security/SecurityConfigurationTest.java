package io.weblinkpilot.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.weblinkpilot.platform.web.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigurationTest {

  private final SecurityConfiguration configuration =
      new SecurityConfiguration(new PlatformSecurityProperties());

  @Test
  void passwordEncoderMatchesEncodedPassword() {
    String encoded = configuration.passwordEncoder().encode("Password1");

    assertThat(configuration.passwordEncoder().matches("Password1", encoded)).isTrue();
  }

  @Test
  void corsConfigurationUsesConfiguredOrigins() {
    CorsProperties corsProperties = new CorsProperties();
    corsProperties.setAllowedOriginPatterns(List.of("http://localhost:5173"));

    CorsConfigurationSource source = configuration.corsConfigurationSource(corsProperties);
    CorsConfiguration corsConfiguration =
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/urls"));

    assertThat(corsConfiguration).isNotNull();
    assertThat(corsConfiguration.getAllowedOriginPatterns())
        .containsExactly("http://localhost:5173");
    assertThat(corsConfiguration.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(corsConfiguration.getAllowedHeaders())
        .containsExactly("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With");
    assertThat(corsConfiguration.getExposedHeaders()).containsExactly("Location");
    assertThat(corsConfiguration.getAllowCredentials()).isTrue();
  }

  @Test
  void corsConfigurationRejectsWildcardOriginsWithCredentials() {
    CorsProperties corsProperties = new CorsProperties();
    corsProperties.setAllowedOriginPatterns(List.of("*"));

    assertThatThrownBy(() -> configuration.corsConfigurationSource(corsProperties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("wildcard");
  }

  @Test
  void corsPropertiesBindCommaSeparatedOriginPatterns() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty(
                "app.cors.allowed-origin-patterns",
                "http://localhost:5173,http://192.168.1.128:5173");

    CorsProperties corsProperties =
        Binder.get(environment).bind("app.cors", Bindable.of(CorsProperties.class)).get();

    assertThat(corsProperties.getAllowedOriginPatterns())
        .containsExactly("http://localhost:5173", "http://192.168.1.128:5173");
  }
}
