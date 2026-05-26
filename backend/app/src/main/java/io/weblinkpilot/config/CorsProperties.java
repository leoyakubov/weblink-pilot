package io.weblinkpilot.config;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

  @NotEmpty
  private List<String> allowedOriginPatterns =
      new ArrayList<>(
          List.of(
              "http://localhost:5173",
              "http://127.0.0.1:5173",
              "http://localhost:4173",
              "http://127.0.0.1:4173",
              "http://localhost:8081",
              "http://127.0.0.1:8081"));

  public List<String> getAllowedOriginPatterns() {
    return List.copyOf(allowedOriginPatterns);
  }

  public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns);
  }
}
