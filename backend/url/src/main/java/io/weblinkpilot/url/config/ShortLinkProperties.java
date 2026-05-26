package io.weblinkpilot.url.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.short-link")
public class ShortLinkProperties {

  private Duration maxExpiration = Duration.ofDays(365);
  private Duration cleanupRetention = Duration.ofDays(30);
  private String cleanupCron = "0 15 3 * * *";

  public Duration getMaxExpiration() {
    return maxExpiration;
  }

  public void setMaxExpiration(Duration maxExpiration) {
    this.maxExpiration = maxExpiration;
  }

  public Duration getCleanupRetention() {
    return cleanupRetention;
  }

  public void setCleanupRetention(Duration cleanupRetention) {
    this.cleanupRetention = cleanupRetention;
  }

  public String getCleanupCron() {
    return cleanupCron;
  }

  public void setCleanupCron(String cleanupCron) {
    this.cleanupCron = cleanupCron;
  }
}
