package io.weblinkpilot.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class PlatformSecurityProperties {

  private boolean publicObservability;

  public boolean isPublicObservability() {
    return publicObservability;
  }

  public void setPublicObservability(boolean publicObservability) {
    this.publicObservability = publicObservability;
  }
}
