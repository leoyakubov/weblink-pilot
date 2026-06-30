package io.weblinkpilot.platform.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

  @NotEmpty private List<String> allowedOriginPatterns = List.of();

  public List<String> getAllowedOriginPatterns() {
    return List.copyOf(allowedOriginPatterns);
  }

  public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = List.copyOf(allowedOriginPatterns);
  }
}
