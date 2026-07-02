package io.weblinkpilot.platform.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.platform.cache.PlatformCacheProperties;
import io.weblinkpilot.platform.rate.RateLimitProperties;
import io.weblinkpilot.platform.security.PlatformSecurityProperties;
import io.weblinkpilot.shared.api.admin.AdminHealthComponentResponse;
import io.weblinkpilot.shared.api.admin.AdminMonitoringResponse;
import io.weblinkpilot.shared.cache.CacheNames;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class AdminMonitoringServiceTest {

  @Test
  void snapshotReportsHealthyLocalRuntime() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    registry.gauge("jvm.threads.live", 12);
    registry.counter("cache.gets").increment(3);
    registry.timer("http.server.requests").record(125, java.util.concurrent.TimeUnit.MILLISECONDS);
    AtomicLong heapUsed = new AtomicLong(128L * 1024 * 1024);
    registry.gauge("jvm.memory.used", Tags.of("area", "heap"), heapUsed, AtomicLong::get);

    ShortLinkRepository shortLinks = mock(ShortLinkRepository.class);
    ClickEventRepository clickEvents = mock(ClickEventRepository.class);
    UserAccountRepository users = mock(UserAccountRepository.class);
    for (String code : java.util.List.of("spring-boot", "vue-js", "postgres", "redis")) {
      when(shortLinks.existsByCode(code)).thenReturn(true);
      when(clickEvents.countByShortCode(code)).thenReturn(2L);
    }
    when(clickEvents.count()).thenReturn(8L);
    when(users.existsByUsername("admin")).thenReturn(true);
    when(users.existsByUsername("user")).thenReturn(true);

    AdminMonitoringResponse snapshot =
        newService(
                registry,
                environment(
                    new String[] {"local"},
                    Map.of(
                        "app.cache.provider", "local",
                        "spring.application.name", "weblink-pilot",
                        "management.endpoints.web.exposure.include", "health,info,prometheus")),
                provider(healthyDataSource("H2")),
                provider(new ConcurrentMapCacheManager(CacheNames.SHORT_LINKS)),
                provider(null),
                provider(mailSender("localhost", 1025)),
                healthyAuthProperties(),
                aiProperties("stub"),
                cacheProperties("local"),
                securityProperties(true),
                rateLimitProperties(true, 20),
                shortLinks,
                clickEvents,
                users)
            .snapshot();

    assertThat(snapshot.metrics())
        .anySatisfy(
            metric -> {
              assertThat(metric.group()).isEqualTo("Threads");
              assertThat(metric.name()).isEqualTo("Live threads");
              assertThat(metric.value()).isEqualTo("12");
            })
        .anySatisfy(
            metric -> {
              assertThat(metric.group()).isEqualTo("HTTP");
              assertThat(metric.name()).isEqualTo("Requests");
              assertThat(metric.value()).isEqualTo("1");
            });
    assertHealth(snapshot, "Database", "UP", "H2");
    assertHealth(snapshot, "Redis", "DISABLED", "Local cache provider is active.");
    assertHealth(snapshot, "Mail", "CONFIGURED", "localhost:1025");
    assertHealth(snapshot, "GitHub OAuth", "UP", "Client credentials are configured.");
    assertHealth(
        snapshot,
        "AI provider",
        "UP",
        "stub provider; deterministic metadata, no external AI calls.");
    assertHealth(
        snapshot,
        "Seeded data",
        "UP",
        "Default users, demo links, and demo analytics are present.");
    assertThat(snapshot.configuration())
        .anySatisfy(
            item -> {
              assertThat(item.name()).isEqualTo("Active profiles");
              assertThat(item.value()).isEqualTo("local");
            });
  }

  @Test
  void snapshotReportsMissingOrDegradedRuntimeParts() {
    AuthProperties auth = healthyAuthProperties();
    auth.setJwtSecret("short-secret");
    auth.setGithubClientId("");
    auth.setGithubClientSecret("");

    ShortLinkRepository shortLinks = mock(ShortLinkRepository.class);
    ClickEventRepository clickEvents = mock(ClickEventRepository.class);
    UserAccountRepository users = mock(UserAccountRepository.class);
    when(clickEvents.count()).thenThrow(new IllegalStateException("analytics unavailable"));

    AdminMonitoringResponse snapshot =
        newService(
                new SimpleMeterRegistry(),
                environment(
                    new String[] {},
                    Map.of(
                        "app.cache.provider", "redis",
                        "spring.application.name", "weblink-pilot")),
                provider(null),
                provider(null),
                provider((RedisConnectionFactory) null),
                provider(null),
                auth,
                aiProperties("openai"),
                cacheProperties("redis"),
                securityProperties(false),
                rateLimitProperties(true, 20),
                shortLinks,
                clickEvents,
                users)
            .snapshot();

    assertHealth(snapshot, "Database", "UNKNOWN", "No datasource bean available.");
    assertHealth(snapshot, "Redis", "UNKNOWN", "Redis connection factory unavailable.");
    assertHealth(snapshot, "Cache", "UNKNOWN", "No cache manager bean available.");
    assertHealth(snapshot, "Mail", "UNKNOWN", "No mail sender bean available.");
    assertHealth(
        snapshot,
        "JWT config",
        "WARNING",
        "JWT secret is configured but shorter than recommended.");
    assertHealth(
        snapshot, "GitHub OAuth", "INFO", "Not configured; username/password auth is available.");
    assertHealth(
        snapshot, "AI provider", "WARNING", "openai provider selected but API key is missing.");
    assertHealth(snapshot, "Analytics storage", "DOWN", "Check failed.");
    assertHealthError(
        snapshot, "Analytics storage", "IllegalStateException", "analytics unavailable");
    assertThat(health(snapshot, "Seeded data").status()).isEqualTo("WARNING");
    assertThat(snapshot.configuration())
        .anySatisfy(
            item -> {
              assertThat(item.name()).isEqualTo("Active profiles");
              assertThat(item.value()).isEqualTo("default");
            });
  }

  private static AdminMonitoringService newService(
      SimpleMeterRegistry registry,
      Environment environment,
      ObjectProvider<DataSource> dataSource,
      ObjectProvider<org.springframework.cache.CacheManager> cacheManager,
      ObjectProvider<RedisConnectionFactory> redisConnectionFactory,
      ObjectProvider<JavaMailSenderImpl> mailSender,
      AuthProperties authProperties,
      AiProperties aiProperties,
      PlatformCacheProperties cacheProperties,
      PlatformSecurityProperties securityProperties,
      RateLimitProperties rateLimitProperties,
      ShortLinkRepository shortLinkRepository,
      ClickEventRepository clickEventRepository,
      UserAccountRepository userAccountRepository) {
    AdminRuntimeMetricsService metricsService = new AdminRuntimeMetricsService(registry);
    AdminHealthService healthService =
        new AdminHealthService(
            dataSource,
            cacheManager,
            redisConnectionFactory,
            mailSender,
            authProperties,
            aiProperties,
            cacheProperties,
            shortLinkRepository,
            clickEventRepository,
            userAccountRepository);
    AdminConfigurationSnapshotService configurationService =
        new AdminConfigurationSnapshotService(
            environment,
            authProperties,
            cacheProperties,
            securityProperties,
            rateLimitProperties,
            shortLinkProperties());
    return new AdminMonitoringService(metricsService, healthService, configurationService);
  }

  private static Environment environment(String[] activeProfiles, Map<String, String> properties) {
    Environment environment = mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(activeProfiles);
    when(environment.getDefaultProfiles()).thenReturn(new String[] {"default"});
    when(environment.getProperty(anyString(), anyString()))
        .thenAnswer(
            invocation ->
                properties.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
    return environment;
  }

  private static DataSource healthyDataSource(String productName) throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    when(metadata.getDatabaseProductName()).thenReturn(productName);
    Connection connection = mock(Connection.class);
    when(connection.isValid(1)).thenReturn(true);
    when(connection.getMetaData()).thenReturn(metadata);
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(connection);
    return dataSource;
  }

  private static JavaMailSenderImpl mailSender(String host, int port) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    return sender;
  }

  private static AuthProperties healthyAuthProperties() {
    AuthProperties properties = new AuthProperties();
    properties.setIssuer("weblink-pilot");
    properties.setJwtSecret("local-development-secret-with-more-than-thirty-two-characters");
    properties.setTokenTtlMinutes(30);
    properties.setGithubClientId("client-id");
    properties.setGithubClientSecret("client-secret");
    properties.setBootstrapAdminUsername("admin");
    properties.setBootstrapUserUsername("user");
    properties.setMailDeliveryMode("SMTP");
    return properties;
  }

  private static PlatformCacheProperties cacheProperties(String provider) {
    PlatformCacheProperties properties = new PlatformCacheProperties();
    properties.setProvider(provider);
    return properties;
  }

  private static AiProperties aiProperties(String provider) {
    AiProperties properties = new AiProperties();
    properties.setProvider(provider);
    return properties;
  }

  private static PlatformSecurityProperties securityProperties(boolean publicObservability) {
    PlatformSecurityProperties properties = new PlatformSecurityProperties();
    properties.setPublicObservability(publicObservability);
    return properties;
  }

  private static RateLimitProperties rateLimitProperties(boolean enabled, int authPerMinute) {
    RateLimitProperties properties = new RateLimitProperties();
    properties.setEnabled(enabled);
    properties.setApiPerMinute(120);
    properties.setRedirectPerMinute(300);
    properties.setAuthPerMinute(authPerMinute);
    return properties;
  }

  private static ShortLinkProperties shortLinkProperties() {
    ShortLinkProperties properties = new ShortLinkProperties();
    properties.setPublicBaseUrl("http://localhost:8080");
    properties.setMaxExpiration(java.time.Duration.ofDays(365));
    properties.setCleanupRetention(java.time.Duration.ofDays(30));
    properties.setCleanupCron("0 15 3 * * *");
    return properties;
  }

  private static <T> ObjectProvider<T> provider(T value) {
    @SuppressWarnings("unchecked")
    ObjectProvider<T> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(value);
    return provider;
  }

  private static void assertHealth(
      AdminMonitoringResponse snapshot, String name, String status, String detail) {
    AdminHealthComponentResponse component = health(snapshot, name);
    assertThat(component.status()).isEqualTo(status);
    assertThat(component.detail()).contains(detail);
  }

  private static void assertHealthError(
      AdminMonitoringResponse snapshot, String name, String type, String message) {
    AdminHealthComponentResponse component = health(snapshot, name);
    assertThat(component.error()).isNotNull();
    assertThat(component.error().type()).isEqualTo(type);
    assertThat(component.error().message()).contains(message);
  }

  private static AdminHealthComponentResponse health(
      AdminMonitoringResponse snapshot, String name) {
    return snapshot.health().stream()
        .filter(component -> component.name().equals(name))
        .findFirst()
        .orElseThrow();
  }
}
