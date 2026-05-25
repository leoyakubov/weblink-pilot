package io.weblinkpilot.config.rate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

  private boolean enabled = true;
  private int apiPerMinute = 120;
  private int redirectPerMinute = 300;

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
