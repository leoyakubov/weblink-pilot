package io.weblinkpilot.config.rate;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

  private boolean enabled;

  @Min(1)
  private int apiPerMinute;

  @Min(1)
  private int redirectPerMinute;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getApiPerMinute() {
    return apiPerMinute;
  }

  public void setApiPerMinute(int apiPerMinute) {
    this.apiPerMinute = apiPerMinute;
  }

  public int getRedirectPerMinute() {
    return redirectPerMinute;
  }

  public void setRedirectPerMinute(int redirectPerMinute) {
    this.redirectPerMinute = redirectPerMinute;
  }
}
