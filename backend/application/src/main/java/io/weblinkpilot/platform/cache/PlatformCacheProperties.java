package io.weblinkpilot.platform.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.cache")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring configuration properties expose nested mutable beans for binding.")
public class PlatformCacheProperties {

  @NotBlank private String provider = CacheProviderNames.LOCAL;
  @Valid private Ttl ttl = new Ttl();

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public boolean isRedis() {
    return CacheProviderNames.REDIS.equalsIgnoreCase(provider);
  }

  public Ttl getTtl() {
    return ttl;
  }

  public void setTtl(Ttl ttl) {
    this.ttl = ttl == null ? new Ttl() : ttl;
  }

  public static class Ttl {

    @NotNull private Duration defaultTtl = Duration.ofMinutes(10);
    @NotNull private Duration shortLinks = Duration.ofMinutes(30);
    @NotNull private Duration analytics = Duration.ofSeconds(30);

    public Duration getDefaultTtl() {
      return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
      this.defaultTtl = defaultTtl;
    }

    public Duration getShortLinks() {
      return shortLinks;
    }

    public void setShortLinks(Duration shortLinks) {
      this.shortLinks = shortLinks;
    }

    public Duration getAnalytics() {
      return analytics;
    }

    public void setAnalytics(Duration analytics) {
      this.analytics = analytics;
    }
  }
}
