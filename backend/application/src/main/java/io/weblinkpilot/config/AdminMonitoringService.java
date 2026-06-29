package io.weblinkpilot.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.shared.contracts.AdminConfigurationItemResponse;
import io.weblinkpilot.shared.contracts.AdminHealthComponentResponse;
import io.weblinkpilot.shared.contracts.AdminMonitoringResponse;
import io.weblinkpilot.shared.contracts.AdminRuntimeMetricResponse;
import java.io.File;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this service.")
public class AdminMonitoringService {

  private final MeterRegistry meterRegistry;
  private final Environment environment;
  private final ObjectProvider<DataSource> dataSource;
  private final ObjectProvider<CacheManager> cacheManager;
  private final ObjectProvider<RedisConnectionFactory> redisConnectionFactory;
  private final ObjectProvider<JavaMailSenderImpl> mailSender;
  private final AuthProperties authProperties;
  private final ShortLinkRepository shortLinkRepository;
  private final ClickEventRepository clickEventRepository;
  private final UserAccountRepository userAccountRepository;

  public AdminMonitoringService(
      MeterRegistry meterRegistry,
      Environment environment,
      ObjectProvider<DataSource> dataSource,
      ObjectProvider<CacheManager> cacheManager,
      ObjectProvider<RedisConnectionFactory> redisConnectionFactory,
      ObjectProvider<JavaMailSenderImpl> mailSender,
      AuthProperties authProperties,
      ShortLinkRepository shortLinkRepository,
      ClickEventRepository clickEventRepository,
      UserAccountRepository userAccountRepository) {
    this.meterRegistry = meterRegistry;
    this.environment = environment;
    this.dataSource = dataSource;
    this.cacheManager = cacheManager;
    this.redisConnectionFactory = redisConnectionFactory;
    this.mailSender = mailSender;
    this.authProperties = authProperties;
    this.shortLinkRepository = shortLinkRepository;
    this.clickEventRepository = clickEventRepository;
    this.userAccountRepository = userAccountRepository;
  }

  public AdminMonitoringResponse snapshot() {
    return new AdminMonitoringResponse(metrics(), health(), configuration());
  }

  private List<AdminRuntimeMetricResponse> metrics() {
    List<AdminRuntimeMetricResponse> metrics = new ArrayList<>();
    addMetric(
        metrics,
        "JVM memory",
        "Heap used",
        bytes(gaugeSum("jvm.memory.used", "area", "heap")),
        "bytes",
        "Current heap memory used.");
    addMetric(
        metrics,
        "JVM memory",
        "Heap max",
        bytes(gaugeSum("jvm.memory.max", "area", "heap")),
        "bytes",
        "Maximum heap memory available.");
    addMetric(
        metrics,
        "Threads",
        "Live threads",
        number(gauge("jvm.threads.live")),
        "threads",
        "Currently live JVM threads.");
    addMetric(
        metrics,
        "Threads",
        "Peak threads",
        number(gauge("jvm.threads.peak")),
        "threads",
        "Peak JVM thread count.");
    addMetric(
        metrics,
        "Garbage collection",
        "GC pause count",
        number(timerCount("jvm.gc.pause")),
        "events",
        "Observed GC pauses.");
    addMetric(
        metrics,
        "Garbage collection",
        "GC pause time",
        millis(timerTotal("jvm.gc.pause")),
        "ms",
        "Total observed GC pause time.");
    addMetric(
        metrics,
        "HTTP",
        "Requests",
        number(timerCount("http.server.requests")),
        "requests",
        "HTTP requests observed by Micrometer.");
    addMetric(
        metrics,
        "HTTP",
        "Request time",
        millis(timerTotal("http.server.requests")),
        "ms",
        "Total HTTP request time.");
    addMetric(
        metrics,
        "Cache",
        "Cache gets",
        number(counter("cache.gets")),
        "ops",
        "Cache lookup operations.");
    addMetric(
        metrics,
        "Cache",
        "Cache puts",
        number(counter("cache.puts")),
        "ops",
        "Cache write operations.");
    addMetric(
        metrics,
        "Datasource",
        "Active connections",
        number(gauge("hikaricp.connections.active")),
        "connections",
        "Active datasource connections.");
    addMetric(
        metrics,
        "Datasource",
        "Idle connections",
        number(gauge("hikaricp.connections.idle")),
        "connections",
        "Idle datasource connections.");
    addMetric(
        metrics,
        "Service counters",
        "Links created",
        number(counter("weblinkpilot.links.created.events")),
        "events",
        "Short-link creation events.");
    addMetric(
        metrics,
        "Service counters",
        "Redirect clicks",
        number(counter("weblinkpilot.links.clicked.events")),
        "events",
        "Redirect click events.");
    addMetric(
        metrics,
        "Service counters",
        "Analytics summaries",
        number(counter("weblinkpilot.analytics.summary.requests")),
        "requests",
        "Analytics summary requests.");
    return metrics;
  }

  private List<AdminHealthComponentResponse> health() {
    List<AdminHealthComponentResponse> health = new ArrayList<>();
    health.add(databaseHealth());
    health.add(redisHealth());
    health.add(cacheHealth());
    health.add(diskHealth());
    health.add(mailHealth());
    health.add(jwtConfigHealth());
    health.add(githubOAuthHealth());
    health.add(analyticsStorageHealth());
    health.add(seededDataHealth());
    return health;
  }

  private List<AdminConfigurationItemResponse> configuration() {
    String[] activeProfiles = environment.getActiveProfiles();
    String profiles =
        activeProfiles.length == 0
            ? String.join(", ", environment.getDefaultProfiles())
            : String.join(", ", activeProfiles);
    return List.of(
        new AdminConfigurationItemResponse(
            "Active profiles", profiles, "Spring profiles active for this runtime."),
        new AdminConfigurationItemResponse(
            "Application name",
            property("spring.application.name", "weblink-pilot"),
            "Spring application name."),
        new AdminConfigurationItemResponse(
            "Cache provider",
            property("app.cache.provider", "local"),
            "Configured cache provider."),
        new AdminConfigurationItemResponse(
            "Public base URL",
            property("app.public-base-url", "not configured"),
            "Public URL used for short links."),
        new AdminConfigurationItemResponse(
            "Frontend base URL",
            property("app.auth.frontend-base-url", "not configured"),
            "Frontend URL used in auth emails and redirects."),
        new AdminConfigurationItemResponse(
            "Rate limiting",
            property("app.rate-limit.enabled", "true"),
            "Whether backend rate limiting is enabled."),
        new AdminConfigurationItemResponse(
            "Auth rate limit",
            property("app.rate-limit.auth-per-minute", "20") + "/min",
            "Throttle for login, refresh, reset, verification, and OAuth completion."),
        new AdminConfigurationItemResponse(
            "Public observability",
            property("app.security.public-observability", "false"),
            "Whether metrics and Prometheus are public for local scraping."),
        new AdminConfigurationItemResponse(
            "Mail delivery mode",
            authProperties.getMailDeliveryMode(),
            "How account emails are delivered."),
        new AdminConfigurationItemResponse(
            "Actuator exposure",
            property("management.endpoints.web.exposure.include", "health, info"),
            "Safe endpoints exposed by this application."),
        new AdminConfigurationItemResponse(
            "Server timezone", ZoneId.systemDefault().toString(), "Timezone used by this JVM."),
        new AdminConfigurationItemResponse(
            "Server time", OffsetDateTime.now().toString(), "Current backend time."),
        new AdminConfigurationItemResponse(
            "Spring env/configprops", "hidden", "Not exposed directly to avoid leaking secrets."));
  }

  private void addMetric(
      List<AdminRuntimeMetricResponse> metrics,
      String group,
      String name,
      String value,
      String unit,
      String description) {
    metrics.add(new AdminRuntimeMetricResponse(group, name, value, unit, description));
  }

  private double gauge(String name) {
    Gauge gauge = meterRegistry.find(name).gauge();
    return gauge == null ? Double.NaN : gauge.value();
  }

  private double gaugeSum(String name, String tagName, String tagValue) {
    return meterRegistry.find(name).tag(tagName, tagValue).gauges().stream()
        .mapToDouble(Gauge::value)
        .sum();
  }

  private double counter(String name) {
    Counter counter = meterRegistry.find(name).counter();
    return counter == null ? Double.NaN : counter.count();
  }

  private long timerCount(String name) {
    Timer timer = meterRegistry.find(name).timer();
    return timer == null ? 0L : timer.count();
  }

  private double timerTotal(String name) {
    Timer timer = meterRegistry.find(name).timer();
    return timer == null ? Double.NaN : timer.totalTime(TimeUnit.MILLISECONDS);
  }

  private String number(double value) {
    if (Double.isNaN(value)) {
      return "N/A";
    }
    return String.format(java.util.Locale.ROOT, "%.0f", value);
  }

  private String number(long value) {
    return Long.toString(value);
  }

  private String bytes(double value) {
    if (Double.isNaN(value) || value <= 0) {
      return "N/A";
    }
    return String.format(java.util.Locale.ROOT, "%.1f MB", value / 1024 / 1024);
  }

  private String millis(double value) {
    if (Double.isNaN(value)) {
      return "N/A";
    }
    return String.format(java.util.Locale.ROOT, "%.0f", value);
  }

  private AdminHealthComponentResponse databaseHealth() {
    DataSource source = dataSource.getIfAvailable();
    if (source == null) {
      return new AdminHealthComponentResponse(
          "Database", "UNKNOWN", "No datasource bean available.");
    }
    try (Connection connection = source.getConnection()) {
      return new AdminHealthComponentResponse(
          "Database",
          connection.isValid(1) ? "UP" : "DOWN",
          connection.getMetaData().getDatabaseProductName());
    } catch (Exception exception) {
      return new AdminHealthComponentResponse("Database", "DOWN", exception.getMessage());
    }
  }

  private AdminHealthComponentResponse diskHealth() {
    File root = new File(".");
    long total = root.getTotalSpace();
    long usable = root.getUsableSpace();
    String detail = total <= 0 ? "Disk information unavailable." : bytes(usable) + " free";
    return new AdminHealthComponentResponse("Disk space", usable > 0 ? "UP" : "UNKNOWN", detail);
  }

  private AdminHealthComponentResponse mailHealth() {
    JavaMailSenderImpl sender = mailSender.getIfAvailable();
    if (sender == null) {
      return new AdminHealthComponentResponse("Mail", "UNKNOWN", "No mail sender bean available.");
    }
    String host = sender.getHost() == null ? "not configured" : sender.getHost();
    return new AdminHealthComponentResponse("Mail", "CONFIGURED", host + ":" + sender.getPort());
  }

  private AdminHealthComponentResponse cacheHealth() {
    CacheManager manager = cacheManager.getIfAvailable();
    if (manager == null) {
      return new AdminHealthComponentResponse(
          "Cache", "UNKNOWN", "No cache manager bean available.");
    }
    return new AdminHealthComponentResponse(
        "Cache", "UP", manager.getClass().getSimpleName() + " " + manager.getCacheNames());
  }

  private AdminHealthComponentResponse redisHealth() {
    if (!"redis".equalsIgnoreCase(property("app.cache.provider", "local"))) {
      return new AdminHealthComponentResponse(
          "Redis", "DISABLED", "Local cache provider is active.");
    }
    RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
    if (factory == null) {
      return new AdminHealthComponentResponse(
          "Redis", "UNKNOWN", "Redis connection factory unavailable.");
    }
    try (RedisConnection connection = factory.getConnection()) {
      return new AdminHealthComponentResponse("Redis", "UP", connection.ping());
    } catch (Exception exception) {
      return new AdminHealthComponentResponse("Redis", "DOWN", exception.getMessage());
    }
  }

  private AdminHealthComponentResponse jwtConfigHealth() {
    String secret = authProperties.getJwtSecret();
    if (secret == null || secret.isBlank()) {
      return new AdminHealthComponentResponse("JWT config", "DOWN", "JWT secret is missing.");
    }
    if (secret.trim().length() < 32) {
      return new AdminHealthComponentResponse(
          "JWT config", "WARNING", "JWT secret is configured but shorter than recommended.");
    }
    return new AdminHealthComponentResponse(
        "JWT config",
        "UP",
        "Issuer "
            + authProperties.getIssuer()
            + ", access token TTL "
            + authProperties.getTokenTtlMinutes()
            + " minutes.");
  }

  private AdminHealthComponentResponse githubOAuthHealth() {
    if (authProperties.isGithubConfigured()) {
      return new AdminHealthComponentResponse(
          "GitHub OAuth", "UP", "Client credentials are configured.");
    }
    return new AdminHealthComponentResponse(
        "GitHub OAuth", "INFO", "Not configured; username/password auth is available.");
  }

  private AdminHealthComponentResponse analyticsStorageHealth() {
    try {
      long events = clickEventRepository.count();
      return new AdminHealthComponentResponse(
          "Analytics storage", "UP", events + " interaction events stored.");
    } catch (Exception exception) {
      return new AdminHealthComponentResponse("Analytics storage", "DOWN", exception.getMessage());
    }
  }

  private AdminHealthComponentResponse seededDataHealth() {
    List<String> expectedLinks = List.of("spring-boot", "vue-js", "postgres", "redis");
    List<String> missingLinks =
        expectedLinks.stream().filter(code -> !shortLinkRepository.existsByCode(code)).toList();
    List<String> missingAnalytics =
        expectedLinks.stream()
            .filter(code -> clickEventRepository.countByShortCode(code) == 0)
            .toList();
    boolean defaultAccountsPresent =
        seededAccountPresent(authProperties.getBootstrapAdminUsername())
            && seededAccountPresent(authProperties.getBootstrapUserUsername());

    if (missingLinks.isEmpty() && missingAnalytics.isEmpty() && defaultAccountsPresent) {
      return new AdminHealthComponentResponse(
          "Seeded data", "UP", "Default users, demo links, and demo analytics are present.");
    }

    List<String> issues = new ArrayList<>();
    if (!missingLinks.isEmpty()) {
      issues.add("missing links: " + String.join(", ", missingLinks));
    }
    if (!missingAnalytics.isEmpty()) {
      issues.add("missing analytics: " + String.join(", ", missingAnalytics));
    }
    if (!defaultAccountsPresent) {
      issues.add("default users missing");
    }
    return new AdminHealthComponentResponse("Seeded data", "WARNING", String.join("; ", issues));
  }

  private boolean seededAccountPresent(String username) {
    return username != null
        && !username.isBlank()
        && userAccountRepository.existsByUsername(username);
  }

  private String property(String key, String fallback) {
    return environment.getProperty(key, fallback);
  }
}
