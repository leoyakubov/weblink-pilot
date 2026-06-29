package io.weblinkpilot.config;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.config.MailProperties;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.web.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AuthProperties.class, MailProperties.class})
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${app.security.public-observability:false}") boolean publicObservability) {
    try {
      http.cors(cors -> {})
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .csrf(csrf -> csrf.disable())
          .headers(
              headers ->
                  headers
                      .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy()))
                      .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
                      .permissionsPolicyHeader(
                          permissions ->
                              permissions.policy(
                                  "camera=(), microphone=(), geolocation=(), payment=()"))
                      .frameOptions(frame -> frame.sameOrigin()))
          .authorizeHttpRequests(
              auth -> {
                auth.requestMatchers("/error", "/actuator/health", "/actuator/info").permitAll();
                if (publicObservability) {
                  auth.requestMatchers(
                          "/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus")
                      .permitAll();
                } else {
                  auth.requestMatchers(
                          "/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus")
                      .hasRole(RoleNames.ADMIN);
                }
                auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/r/**", "/q/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/password-reset/request",
                        "/api/v1/auth/password-reset/confirm",
                        "/api/v1/auth/email-verification/request",
                        "/api/v1/auth/email-verification/confirm",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/oauth2/github/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/me")
                    .authenticated()
                    .requestMatchers("/api/v1/auth/account", "/api/v1/auth/account/password")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/analytics/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/ai/links/*/metadata")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/ai/links/*/metadata/regenerate")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole(RoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, "/api/v1/urls")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/urls/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/urls")
                    .permitAll()
                    .requestMatchers("/api/v1/urls/*/preview")
                    .permitAll()
                    .requestMatchers("/api/v1/urls/*/qr")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll();
              })
          .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      return http.build();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to configure security", exception);
    }
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    java.util.List<String> allowedOriginPatterns = corsProperties.getAllowedOriginPatterns();
    if (allowedOriginPatterns.stream().anyMatch(this::isWildcardOrigin)) {
      throw new IllegalStateException("CORS wildcard origins are not allowed with credentials");
    }
    configuration.setAllowedOriginPatterns(allowedOriginPatterns);
    configuration.setAllowedMethods(
        java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        java.util.List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
    configuration.setExposedHeaders(java.util.List.of("Location"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private String contentSecurityPolicy() {
    return "default-src 'self'; "
        + "base-uri 'self'; "
        + "object-src 'none'; "
        + "frame-ancestors 'none'; "
        + "img-src 'self' data: https:; "
        + "font-src 'self' data:; "
        + "style-src 'self' 'unsafe-inline'; "
        + "script-src 'self'; "
        + "connect-src 'self' http://localhost:* http://127.0.0.1:* https:";
  }

  private boolean isWildcardOrigin(String originPattern) {
    return originPattern == null || originPattern.isBlank() || "*".equals(originPattern.trim());
  }
}
