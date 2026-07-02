package io.weblinkpilot.platform.admin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.platform.cache.PlatformCacheProperties;
import io.weblinkpilot.shared.api.admin.AdminHealthComponentResponse;
import io.weblinkpilot.shared.api.admin.AdminHealthErrorResponse;
import io.weblinkpilot.shared.seed.DemoSeedDataCatalog;
import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this service.")
public class AdminHealthService {

  private final ObjectProvider<DataSource> dataSource;
  private final ObjectProvider<CacheManager> cacheManager;
  private final ObjectProvider<RedisConnectionFactory> redisConnectionFactory;
  private final ObjectProvider<JavaMailSenderImpl> mailSender;
  private final AuthProperties authProperties;
  private final AiProperties aiProperties;
  private final PlatformCacheProperties cacheProperties;
  private final ShortLinkRepository shortLinkRepository;
  private final ClickEventRepository clickEventRepository;
  private final UserAccountRepository userAccountRepository;

  public AdminHealthService(
      ObjectProvider<DataSource> dataSource,
      ObjectProvider<CacheManager> cacheManager,
      ObjectProvider<RedisConnectionFactory> redisConnectionFactory,
      ObjectProvider<JavaMailSenderImpl> mailSender,
      AuthProperties authProperties,
      AiProperties aiProperties,
      PlatformCacheProperties cacheProperties,
      ShortLinkRepository shortLinkRepository,
      ClickEventRepository clickEventRepository,
      UserAccountRepository userAccountRepository) {
    this.dataSource = dataSource;
    this.cacheManager = cacheManager;
    this.redisConnectionFactory = redisConnectionFactory;
    this.mailSender = mailSender;
    this.authProperties = authProperties;
    this.aiProperties = aiProperties;
    this.cacheProperties = cacheProperties;
    this.shortLinkRepository = shortLinkRepository;
    this.clickEventRepository = clickEventRepository;
    this.userAccountRepository = userAccountRepository;
  }

  public List<AdminHealthComponentResponse> health() {
    return List.of(
        databaseHealth(),
        redisHealth(),
        cacheHealth(),
        diskHealth(),
        mailHealth(),
        jwtConfigHealth(),
        githubOAuthHealth(),
        aiProviderHealth(),
        analyticsStorageHealth(),
        seededDataHealth());
  }

  private AdminHealthComponentResponse databaseHealth() {
    DataSource source = dataSource.getIfAvailable();
    if (source == null) {
      return health(
          HealthComponent.DATABASE, AdminHealthStatus.UNKNOWN, "No datasource bean available.");
    }
    try (Connection connection = source.getConnection()) {
      return health(
          HealthComponent.DATABASE,
          connection.isValid(1) ? AdminHealthStatus.UP.value() : AdminHealthStatus.DOWN.value(),
          connection.getMetaData().getDatabaseProductName());
    } catch (Exception exception) {
      return failedHealth(HealthComponent.DATABASE, exception);
    }
  }

  private AdminHealthComponentResponse diskHealth() {
    File root = new File(".");
    long total = root.getTotalSpace();
    long usable = root.getUsableSpace();
    String detail = total <= 0 ? "Disk information unavailable." : bytes(usable) + " free";
    return health(
        HealthComponent.DISK_SPACE,
        usable > 0 ? AdminHealthStatus.UP.value() : AdminHealthStatus.UNKNOWN.value(),
        detail);
  }

  private AdminHealthComponentResponse mailHealth() {
    JavaMailSenderImpl sender = mailSender.getIfAvailable();
    if (sender == null) {
      return health(
          HealthComponent.MAIL, AdminHealthStatus.UNKNOWN, "No mail sender bean available.");
    }
    String host = sender.getHost() == null ? "not configured" : sender.getHost();
    return health(
        HealthComponent.MAIL, AdminHealthStatus.CONFIGURED, host + ":" + sender.getPort());
  }

  private AdminHealthComponentResponse cacheHealth() {
    CacheManager manager = cacheManager.getIfAvailable();
    if (manager == null) {
      return health(
          HealthComponent.CACHE, AdminHealthStatus.UNKNOWN, "No cache manager bean available.");
    }
    return health(
        HealthComponent.CACHE,
        AdminHealthStatus.UP,
        manager.getClass().getSimpleName() + " " + manager.getCacheNames());
  }

  private AdminHealthComponentResponse redisHealth() {
    if (!cacheProperties.isRedis()) {
      return health(
          HealthComponent.REDIS, AdminHealthStatus.DISABLED, "Local cache provider is active.");
    }
    RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
    if (factory == null) {
      return health(
          HealthComponent.REDIS,
          AdminHealthStatus.UNKNOWN,
          "Redis connection factory unavailable.");
    }
    try (RedisConnection connection = factory.getConnection()) {
      return health(HealthComponent.REDIS, AdminHealthStatus.UP, connection.ping());
    } catch (Exception exception) {
      return failedHealth(HealthComponent.REDIS, exception);
    }
  }

  private AdminHealthComponentResponse jwtConfigHealth() {
    String secret = authProperties.getJwtSecret();
    if (secret == null || secret.isBlank()) {
      return health(HealthComponent.JWT_CONFIG, AdminHealthStatus.DOWN, "JWT secret is missing.");
    }
    if (secret.trim().length() < 32) {
      return health(
          HealthComponent.JWT_CONFIG,
          AdminHealthStatus.WARNING,
          "JWT secret is configured but shorter than recommended.");
    }
    return health(
        HealthComponent.JWT_CONFIG,
        AdminHealthStatus.UP,
        "Issuer "
            + authProperties.getIssuer()
            + ", access token TTL "
            + authProperties.getTokenTtlMinutes()
            + " minutes.");
  }

  private AdminHealthComponentResponse githubOAuthHealth() {
    if (authProperties.isGithubConfigured()) {
      return health(
          HealthComponent.GITHUB_OAUTH, AdminHealthStatus.UP, "Client credentials are configured.");
    }
    return health(
        HealthComponent.GITHUB_OAUTH,
        AdminHealthStatus.INFO,
        "Not configured; username/password auth is available.");
  }

  private AdminHealthComponentResponse aiProviderHealth() {
    if (!aiProperties.isEnabled()) {
      return health(
          HealthComponent.AI_PROVIDER,
          AdminHealthStatus.DISABLED,
          "AI metadata enrichment is disabled.");
    }

    String provider = aiProperties.getProvider().trim().toLowerCase(Locale.ROOT);
    return switch (provider) {
      case "stub" ->
          health(
              HealthComponent.AI_PROVIDER,
              AdminHealthStatus.UP,
              "stub provider; deterministic metadata, no external AI calls.");
      case "ollama" ->
          health(
              HealthComponent.AI_PROVIDER,
              AdminHealthStatus.CONFIGURED,
              "ollama provider configured at "
                  + aiProperties.getOllama().getBaseUrl()
                  + " using model "
                  + aiProperties.getOllama().getModel()
                  + ".");
      case "openai" -> openAiProviderHealth();
      default ->
          health(
              HealthComponent.AI_PROVIDER,
              AdminHealthStatus.WARNING,
              "Unsupported AI provider configured: " + aiProperties.getProvider() + ".");
    };
  }

  private AdminHealthComponentResponse openAiProviderHealth() {
    if (aiProperties.getOpenai().getApiKey() == null
        || aiProperties.getOpenai().getApiKey().isBlank()) {
      return health(
          HealthComponent.AI_PROVIDER,
          AdminHealthStatus.WARNING,
          "openai provider selected but API key is missing.");
    }
    return health(
        HealthComponent.AI_PROVIDER,
        AdminHealthStatus.CONFIGURED,
        "openai-compatible provider configured at "
            + aiProperties.getOpenai().getBaseUrl()
            + " using model "
            + aiProperties.getOpenai().getModel()
            + ".");
  }

  private AdminHealthComponentResponse analyticsStorageHealth() {
    try {
      long events = clickEventRepository.count();
      return health(
          HealthComponent.ANALYTICS_STORAGE,
          AdminHealthStatus.UP,
          events + " interaction events stored.");
    } catch (Exception exception) {
      return failedHealth(HealthComponent.ANALYTICS_STORAGE, exception);
    }
  }

  private AdminHealthComponentResponse seededDataHealth() {
    List<String> expectedLinks = DemoSeedDataCatalog.codes();
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
      return health(
          HealthComponent.SEEDED_DATA,
          AdminHealthStatus.UP,
          "Default users, demo links, and demo analytics are present.");
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
    return health(
        HealthComponent.SEEDED_DATA, AdminHealthStatus.WARNING, String.join("; ", issues));
  }

  private boolean seededAccountPresent(String username) {
    return username != null
        && !username.isBlank()
        && userAccountRepository.existsByUsername(username);
  }

  private String bytes(double value) {
    if (Double.isNaN(value) || value <= 0) {
      return "N/A";
    }
    return String.format(Locale.ROOT, "%.1f MB", value / 1024 / 1024);
  }

  private AdminHealthComponentResponse health(
      HealthComponent component, AdminHealthStatus status, String detail) {
    return new AdminHealthComponentResponse(component.displayName(), status.value(), detail);
  }

  private AdminHealthComponentResponse health(
      HealthComponent component, String status, String detail) {
    return new AdminHealthComponentResponse(component.displayName(), status, detail);
  }

  private AdminHealthComponentResponse failedHealth(
      HealthComponent component, Exception exception) {
    return new AdminHealthComponentResponse(
        component.displayName(),
        AdminHealthStatus.DOWN.value(),
        "Check failed.",
        new AdminHealthErrorResponse(
            exception.getClass().getSimpleName(), exceptionMessage(exception)));
  }

  private String exceptionMessage(Exception exception) {
    String message = exception.getMessage();
    return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
  }

  private enum HealthComponent {
    DATABASE("Database"),
    DISK_SPACE("Disk space"),
    MAIL("Mail"),
    CACHE("Cache"),
    REDIS("Redis"),
    JWT_CONFIG("JWT config"),
    GITHUB_OAUTH("GitHub OAuth"),
    AI_PROVIDER("AI provider"),
    ANALYTICS_STORAGE("Analytics storage"),
    SEEDED_DATA("Seeded data");

    private final String displayName;

    HealthComponent(String displayName) {
      this.displayName = displayName;
    }

    private String displayName() {
      return displayName;
    }
  }
}
