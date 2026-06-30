package io.weblinkpilot.platform.admin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.platform.PlatformPropertyKeys;
import io.weblinkpilot.platform.cache.PlatformCacheProperties;
import io.weblinkpilot.platform.rate.RateLimitProperties;
import io.weblinkpilot.platform.security.PlatformSecurityProperties;
import io.weblinkpilot.shared.api.admin.AdminConfigurationItemResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "Spring Environment and auth properties are framework-managed configuration collaborators.")
public class AdminConfigurationSnapshotService {

  private static final String HIDDEN = "hidden";

  private final Environment environment;
  private final AuthProperties authProperties;
  private final PlatformCacheProperties cacheProperties;
  private final PlatformSecurityProperties securityProperties;
  private final RateLimitProperties rateLimitProperties;
  private final ShortLinkProperties shortLinkProperties;

  public AdminConfigurationSnapshotService(
      Environment environment,
      AuthProperties authProperties,
      PlatformCacheProperties cacheProperties,
      PlatformSecurityProperties securityProperties,
      RateLimitProperties rateLimitProperties,
      ShortLinkProperties shortLinkProperties) {
    this.environment = environment;
    this.authProperties = authProperties;
    this.cacheProperties = cacheProperties;
    this.securityProperties = securityProperties;
    this.rateLimitProperties = rateLimitProperties;
    this.shortLinkProperties = shortLinkProperties;
  }

  public List<AdminConfigurationItemResponse> configuration() {
    return Arrays.stream(ConfigurationItem.values()).map(item -> item.response(this)).toList();
  }

  private String activeProfiles() {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0
        ? String.join(", ", environment.getDefaultProfiles())
        : String.join(", ", activeProfiles);
  }

  private String property(String key, String fallback) {
    return environment.getProperty(key, fallback);
  }

  private enum ConfigurationItem {
    ACTIVE_PROFILES(
        "Active profiles",
        "Spring profiles active for this runtime.",
        AdminConfigurationSnapshotService::activeProfiles),
    APPLICATION_NAME(
        "Application name",
        "Spring application name.",
        service -> service.property(PlatformPropertyKeys.SPRING_APPLICATION_NAME, "weblink-pilot")),
    CACHE_PROVIDER(
        "Cache provider",
        "Configured cache provider.",
        service -> service.cacheProperties.getProvider()),
    PUBLIC_BASE_URL(
        "Public base URL",
        "Public URL used for short links.",
        service -> service.shortLinkProperties.getPublicBaseUrl()),
    FRONTEND_BASE_URL(
        "Frontend base URL",
        "Frontend URL used in auth emails and redirects.",
        service -> service.authProperties.getFrontendBaseUrl()),
    RATE_LIMITING(
        "Rate limiting",
        "Whether backend rate limiting is enabled.",
        service -> Boolean.toString(service.rateLimitProperties.isEnabled())),
    AUTH_RATE_LIMIT(
        "Auth rate limit",
        "Throttle for login, refresh, reset, verification, and OAuth completion.",
        service -> service.rateLimitProperties.getAuthPerMinute() + "/min"),
    PUBLIC_OBSERVABILITY(
        "Public observability",
        "Whether metrics and Prometheus are public for local scraping.",
        service -> Boolean.toString(service.securityProperties.isPublicObservability())),
    MAIL_DELIVERY_MODE(
        "Mail delivery mode",
        "How account emails are delivered.",
        service -> service.authProperties.getMailDeliveryMode()),
    ACTUATOR_EXPOSURE(
        "Actuator exposure",
        "Safe endpoints exposed by this application.",
        service ->
            service.property(PlatformPropertyKeys.ACTUATOR_EXPOSURE_INCLUDE, "health, info")),
    SERVER_TIMEZONE(
        "Server timezone",
        "Timezone used by this JVM.",
        service -> ZoneId.systemDefault().toString()),
    SERVER_TIME("Server time", "Current backend time.", service -> OffsetDateTime.now().toString()),
    SPRING_ENV_CONFIGPROPS(
        "Spring env/configprops",
        "Not exposed directly to avoid leaking secrets.",
        service -> HIDDEN);

    private final String name;
    private final String description;
    private final ConfigurationValueResolver valueResolver;

    ConfigurationItem(String name, String description, ConfigurationValueResolver valueResolver) {
      this.name = name;
      this.description = description;
      this.valueResolver = valueResolver;
    }

    private AdminConfigurationItemResponse response(AdminConfigurationSnapshotService service) {
      return new AdminConfigurationItemResponse(name, valueResolver.resolve(service), description);
    }
  }

  @FunctionalInterface
  private interface ConfigurationValueResolver {
    String resolve(AdminConfigurationSnapshotService service);
  }
}
