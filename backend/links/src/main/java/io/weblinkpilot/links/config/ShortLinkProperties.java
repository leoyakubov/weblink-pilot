package io.weblinkpilot.links.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Component
@Validated
@ConfigurationProperties(prefix = "app.short-link")
public class ShortLinkProperties {

  @NotNull private Duration maxExpiration;
  @NotNull private Duration cleanupRetention;
  @NotBlank private String cleanupCron;

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
