package io.weblinkpilot.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

  @NotBlank private String issuer = AuthDefaults.ISSUER;
  @NotBlank private String jwtSecret;
  @Min(15) private long tokenTtlMinutes = AuthDefaults.TOKEN_TTL_MINUTES;
  @Min(1) private long refreshTokenTtlDays = AuthDefaults.REFRESH_TOKEN_TTL_DAYS;
  @NotBlank private String refreshCookieName = AuthDefaults.REFRESH_COOKIE_NAME;
  @NotBlank private String refreshCookiePath = AuthDefaults.REFRESH_COOKIE_PATH;
  @Pattern(regexp = "(?i)STRICT|LAX|NONE")
  private String refreshCookieSameSite = AuthDefaults.REFRESH_COOKIE_SAME_SITE;
  private boolean refreshCookieSecure = AuthDefaults.REFRESH_COOKIE_SECURE;
  private String bootstrapAdminUsername = BootstrapDefaults.ADMIN_USERNAME;
  private String bootstrapAdminPassword = BootstrapDefaults.ADMIN_PASSWORD;
  private String bootstrapAdminRole = BootstrapDefaults.ADMIN_ROLE;
  private String bootstrapUserUsername = BootstrapDefaults.USER_USERNAME;
  private String bootstrapUserPassword = BootstrapDefaults.USER_PASSWORD;
  private String bootstrapUserRole = BootstrapDefaults.USER_ROLE;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getTokenTtlMinutes() {
    return tokenTtlMinutes;
  }

  public void setTokenTtlMinutes(long tokenTtlMinutes) {
    this.tokenTtlMinutes = tokenTtlMinutes;
  }

  public long getRefreshTokenTtlDays() {
    return refreshTokenTtlDays;
  }

  public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }

  public String getRefreshCookieName() {
    return refreshCookieName;
  }

  public void setRefreshCookieName(String refreshCookieName) {
    this.refreshCookieName = refreshCookieName;
  }

  public String getRefreshCookiePath() {
    return refreshCookiePath;
  }

  public void setRefreshCookiePath(String refreshCookiePath) {
    this.refreshCookiePath = refreshCookiePath;
  }

  public String getRefreshCookieSameSite() {
    return refreshCookieSameSite;
  }

  public void setRefreshCookieSameSite(String refreshCookieSameSite) {
    this.refreshCookieSameSite = refreshCookieSameSite;
  }

  public boolean isRefreshCookieSecure() {
    return refreshCookieSecure;
  }

  public void setRefreshCookieSecure(boolean refreshCookieSecure) {
    this.refreshCookieSecure = refreshCookieSecure;
  }

  public String getBootstrapAdminUsername() {
    return bootstrapAdminUsername;
  }

  public void setBootstrapAdminUsername(String bootstrapAdminUsername) {
    this.bootstrapAdminUsername = bootstrapAdminUsername;
  }

  public String getBootstrapAdminPassword() {
    return bootstrapAdminPassword;
  }

  public void setBootstrapAdminPassword(String bootstrapAdminPassword) {
    this.bootstrapAdminPassword = bootstrapAdminPassword;
  }

  public String getBootstrapAdminRole() {
    return bootstrapAdminRole;
  }

  public void setBootstrapAdminRole(String bootstrapAdminRole) {
    this.bootstrapAdminRole = bootstrapAdminRole;
  }

  public String getBootstrapUserUsername() {
    return bootstrapUserUsername;
  }

  public void setBootstrapUserUsername(String bootstrapUserUsername) {
    this.bootstrapUserUsername = bootstrapUserUsername;
  }

  public String getBootstrapUserPassword() {
    return bootstrapUserPassword;
  }

  public void setBootstrapUserPassword(String bootstrapUserPassword) {
    this.bootstrapUserPassword = bootstrapUserPassword;
  }

  public String getBootstrapUserRole() {
    return bootstrapUserRole;
  }

  public void setBootstrapUserRole(String bootstrapUserRole) {
    this.bootstrapUserRole = bootstrapUserRole;
  }
}
