package io.weblinkpilot.links.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.short-link")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring configuration properties expose nested mutable beans for binding.")
public class ShortLinkProperties {

  @NotBlank private String publicBaseUrl;
  @NotNull private Duration maxExpiration;
  @NotNull private Duration cleanupRetention;
  @NotBlank private String cleanupCron;
  @Valid @NotNull private Alias alias = new Alias();
  @Valid @NotNull private Browse browse = new Browse();
  @Valid @NotNull private Code code = new Code();
  @Valid @NotNull private Qr qr = new Qr();

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

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

  public Alias getAlias() {
    return alias;
  }

  public void setAlias(Alias alias) {
    this.alias = alias;
  }

  public Browse getBrowse() {
    return browse;
  }

  public void setBrowse(Browse browse) {
    this.browse = browse;
  }

  public Code getCode() {
    return code;
  }

  public void setCode(Code code) {
    this.code = code;
  }

  public Qr getQr() {
    return qr;
  }

  public void setQr(Qr qr) {
    this.qr = qr;
  }

  public static class Alias {

    @Min(1)
    private int minLength = 3;

    @Min(1)
    private int maxLength = 64;

    @NotBlank private String allowedPattern = "^[A-Za-z0-9_-]+$";

    public int getMinLength() {
      return minLength;
    }

    public void setMinLength(int minLength) {
      this.minLength = minLength;
    }

    public int getMaxLength() {
      return maxLength;
    }

    public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
    }

    public String getAllowedPattern() {
      return allowedPattern;
    }

    public void setAllowedPattern(String allowedPattern) {
      this.allowedPattern = allowedPattern;
    }
  }

  public static class Browse {

    @Min(1)
    private int defaultLimit = 10;

    @Min(1)
    private int maxLimit = 50;

    public int getDefaultLimit() {
      return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
      this.defaultLimit = defaultLimit;
    }

    public int getMaxLimit() {
      return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
      this.maxLimit = maxLimit;
    }
  }

  public static class Code {

    @Min(1)
    private int length = 7;

    @Min(1)
    private int maxGenerationAttempts = 10;

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public int getMaxGenerationAttempts() {
      return maxGenerationAttempts;
    }

    public void setMaxGenerationAttempts(int maxGenerationAttempts) {
      this.maxGenerationAttempts = maxGenerationAttempts;
    }
  }

  public static class Qr {

    @Min(1)
    private int size = 320;

    @Min(0)
    private int margin = 2;

    @NotBlank private String imageFormat = "PNG";

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }

    public int getMargin() {
      return margin;
    }

    public void setMargin(int margin) {
      this.margin = margin;
    }

    public String getImageFormat() {
      return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
      this.imageFormat = imageFormat;
    }
  }
}
