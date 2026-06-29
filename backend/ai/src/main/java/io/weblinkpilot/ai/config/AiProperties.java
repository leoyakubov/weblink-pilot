package io.weblinkpilot.ai.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Nested configuration properties are bound and managed by Spring.")
public class AiProperties {

  private boolean enabled = true;

  @NotBlank private String provider = "stub";

  @NotBlank private String promptVersion = "link-metadata-v1";

  private Ollama ollama = new Ollama();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getPromptVersion() {
    return promptVersion;
  }

  public void setPromptVersion(String promptVersion) {
    this.promptVersion = promptVersion;
  }

  public Ollama getOllama() {
    return ollama;
  }

  public void setOllama(Ollama ollama) {
    this.ollama = ollama;
  }

  public static class Ollama {

    @NotBlank private String baseUrl = "http://localhost:11434";

    @NotBlank private String model = "llama3.2:1b";

    private Duration timeout = Duration.ofSeconds(60);

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }
  }
}
