package io.weblinkpilot.platform.security;

import static io.weblinkpilot.platform.web.ApiRoutes.API_DOCUMENTATION_PATHS;
import static io.weblinkpilot.platform.web.ApiRoutes.OBSERVABILITY_PATHS;
import static io.weblinkpilot.platform.web.ApiRoutes.PUBLIC_ACTUATOR_PATHS;
import static io.weblinkpilot.platform.web.ApiRoutes.PUBLIC_REDIRECT_PATHS;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.config.MailProperties;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.web.security.JwtAuthenticationFilter;
import io.weblinkpilot.platform.web.ApiRoutes;
import io.weblinkpilot.platform.web.CorsProperties;
import io.weblinkpilot.platform.web.PlatformHttpHeaders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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

  private static final String CORS_WILDCARD_ORIGIN = "*";
  private static final String SECURITY_CONFIGURATION_ERROR = "Unable to configure security";
  private static final String CORS_WILDCARD_ERROR =
      "CORS wildcard origins are not allowed with credentials";
  private static final String CSP_DEFAULT_SRC_SELF = "default-src 'self'";
  private static final String CSP_BASE_URI_SELF = "base-uri 'self'";
  private static final String CSP_OBJECT_SRC_NONE = "object-src 'none'";
  private static final String CSP_FRAME_ANCESTORS_NONE = "frame-ancestors 'none'";
  private static final String CSP_IMG_SRC = "img-src 'self' data: https:";
  private static final String CSP_FONT_SRC = "font-src 'self' data:";
  private static final String CSP_STYLE_SRC = "style-src 'self' 'unsafe-inline'";
  private static final String CSP_SCRIPT_SRC = "script-src 'self'";
  private static final String CSP_CONNECT_SRC =
      "connect-src 'self' http://localhost:* http://127.0.0.1:* https:";
  private static final String PERMISSIONS_POLICY =
      "camera=(), microphone=(), geolocation=(), payment=()";
  private static final java.util.List<String> ALLOWED_CORS_METHODS =
      java.util.List.of(
          HttpMethod.GET.name(),
          HttpMethod.POST.name(),
          HttpMethod.PUT.name(),
          HttpMethod.PATCH.name(),
          HttpMethod.DELETE.name(),
          HttpMethod.OPTIONS.name());
  private static final java.util.List<String> ALLOWED_CORS_HEADERS =
      java.util.List.of(
          HttpHeaders.AUTHORIZATION,
          HttpHeaders.CONTENT_TYPE,
          HttpHeaders.ACCEPT,
          HttpHeaders.ORIGIN,
          PlatformHttpHeaders.X_REQUESTED_WITH);
  private static final java.util.List<String> EXPOSED_CORS_HEADERS =
      java.util.List.of(HttpHeaders.LOCATION);

  private final PlatformSecurityProperties securityProperties;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring singleton configuration stores injected configuration properties.")
  public SecurityConfiguration(PlatformSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) {
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
                          permissions -> permissions.policy(PERMISSIONS_POLICY))
                      .frameOptions(frame -> frame.sameOrigin()))
          .authorizeHttpRequests(
              auth -> {
                auth.requestMatchers(ApiRoutes.ERROR).permitAll();
                auth.requestMatchers(PUBLIC_ACTUATOR_PATHS.toArray(String[]::new)).permitAll();
                if (securityProperties.isPublicObservability()) {
                  auth.requestMatchers(OBSERVABILITY_PATHS.toArray(String[]::new)).permitAll();
                } else {
                  auth.requestMatchers(OBSERVABILITY_PATHS.toArray(String[]::new))
                      .hasRole(RoleNames.ADMIN);
                }
                auth.requestMatchers(API_DOCUMENTATION_PATHS.toArray(String[]::new))
                    .permitAll()
                    .requestMatchers(PUBLIC_REDIRECT_PATHS.toArray(String[]::new))
                    .permitAll()
                    .requestMatchers(ApiRoutes.PUBLIC_AUTH_PATHS.toArray(String[]::new))
                    .permitAll()
                    .requestMatchers(ApiRoutes.AUTH_GITHUB_OAUTH_PATTERN)
                    .permitAll()
                    .requestMatchers(ApiRoutes.AUTH_ME)
                    .authenticated()
                    .requestMatchers(ApiRoutes.AUTH_ACCOUNT, ApiRoutes.AUTH_ACCOUNT_PASSWORD)
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, ApiRoutes.ANALYTICS_PATTERN)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, ApiRoutes.AI_LINK_METADATA_PATTERN)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, ApiRoutes.AI_LINK_METADATA_REGENERATE_PATTERN)
                    .authenticated()
                    .requestMatchers(ApiRoutes.ADMIN_PATTERN)
                    .hasRole(RoleNames.ADMIN)
                    .requestMatchers(HttpMethod.GET, ApiRoutes.URLS_COLLECTION)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, ApiRoutes.URLS_ITEM_PATTERN)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, ApiRoutes.URLS_COLLECTION)
                    .permitAll()
                    .requestMatchers(ApiRoutes.URLS_PREVIEW_PATTERN)
                    .permitAll()
                    .requestMatchers(ApiRoutes.URLS_QR_PATTERN)
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, ApiRoutes.ALL_PATHS_PATTERN)
                    .permitAll()
                    .anyRequest()
                    .denyAll();
              })
          .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      return http.build();
    } catch (Exception exception) {
      throw new IllegalStateException(SECURITY_CONFIGURATION_ERROR, exception);
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
      throw new IllegalStateException(CORS_WILDCARD_ERROR);
    }
    configuration.setAllowedOriginPatterns(allowedOriginPatterns);
    configuration.setAllowedMethods(ALLOWED_CORS_METHODS);
    configuration.setAllowedHeaders(ALLOWED_CORS_HEADERS);
    configuration.setExposedHeaders(EXPOSED_CORS_HEADERS);
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(ApiRoutes.ALL_PATHS_PATTERN, configuration);
    return source;
  }

  private String contentSecurityPolicy() {
    return String.join(
        "; ",
        CSP_DEFAULT_SRC_SELF,
        CSP_BASE_URI_SELF,
        CSP_OBJECT_SRC_NONE,
        CSP_FRAME_ANCESTORS_NONE,
        CSP_IMG_SRC,
        CSP_FONT_SRC,
        CSP_STYLE_SRC,
        CSP_SCRIPT_SRC,
        CSP_CONNECT_SRC);
  }

  private boolean isWildcardOrigin(String originPattern) {
    return originPattern == null
        || originPattern.isBlank()
        || CORS_WILDCARD_ORIGIN.equals(originPattern.trim());
  }
}
