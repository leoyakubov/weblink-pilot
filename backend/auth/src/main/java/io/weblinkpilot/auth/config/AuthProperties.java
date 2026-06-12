package io.weblinkpilot.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

  @NotBlank private String issuer;
  @NotBlank private String jwtSecret;

  @Min(15)
  private long tokenTtlMinutes;

  @Min(1)
  private long refreshTokenTtlDays;

  @Min(1)
  private long accountActionTokenTtlHours;

  @Min(1)
  private long githubLoginTicketTtlMinutes;

  @NotBlank private String refreshCookieName;
  @NotBlank private String refreshCookiePath;
  @NotBlank private String frontendBaseUrl;
  private String githubClientId;
  private String githubClientSecret;
  @NotBlank private String githubScope;
  @NotBlank private String githubStateCookieName;
  @NotBlank private String githubStateCookiePath;

  @Pattern(regexp = "(?i)STRICT|LAX|NONE")
  private String refreshCookieSameSite;

  private boolean refreshCookieSecure;
  private String bootstrapAdminUsername;
  private String bootstrapAdminPassword;
  private String bootstrapAdminRole;
  private String bootstrapAdminEmail;
  private String bootstrapUserUsername;
  private String bootstrapUserPassword;
  private String bootstrapUserRole;
  private String bootstrapUserEmail;

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

  public long getAccountActionTokenTtlHours() {
    return accountActionTokenTtlHours;
  }

  public void setAccountActionTokenTtlHours(long accountActionTokenTtlHours) {
    this.accountActionTokenTtlHours = accountActionTokenTtlHours;
  }

  public long getGithubLoginTicketTtlMinutes() {
    return githubLoginTicketTtlMinutes;
  }

  public void setGithubLoginTicketTtlMinutes(long githubLoginTicketTtlMinutes) {
    this.githubLoginTicketTtlMinutes = githubLoginTicketTtlMinutes;
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

  public String getFrontendBaseUrl() {
    return frontendBaseUrl;
  }

  public void setFrontendBaseUrl(String frontendBaseUrl) {
    this.frontendBaseUrl = frontendBaseUrl;
  }

  public String getGithubClientId() {
    return githubClientId;
  }

  public void setGithubClientId(String githubClientId) {
    this.githubClientId = githubClientId;
  }

  public String getGithubClientSecret() {
    return githubClientSecret;
  }

  public void setGithubClientSecret(String githubClientSecret) {
    this.githubClientSecret = githubClientSecret;
  }

  public String getGithubScope() {
    return githubScope;
  }

  public void setGithubScope(String githubScope) {
    this.githubScope = githubScope;
  }

  public String getGithubStateCookieName() {
    return githubStateCookieName;
  }

  public void setGithubStateCookieName(String githubStateCookieName) {
    this.githubStateCookieName = githubStateCookieName;
  }

  public String getGithubStateCookiePath() {
    return githubStateCookiePath;
  }

  public void setGithubStateCookiePath(String githubStateCookiePath) {
    this.githubStateCookiePath = githubStateCookiePath;
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

  public String getBootstrapAdminEmail() {
    return bootstrapAdminEmail;
  }

  public void setBootstrapAdminEmail(String bootstrapAdminEmail) {
    this.bootstrapAdminEmail = bootstrapAdminEmail;
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

  public String getBootstrapUserEmail() {
    return bootstrapUserEmail;
  }

  public void setBootstrapUserEmail(String bootstrapUserEmail) {
    this.bootstrapUserEmail = bootstrapUserEmail;
  }

  public boolean isGithubConfigured() {
    return githubClientId != null
        && !githubClientId.isBlank()
        && githubClientSecret != null
        && !githubClientSecret.isBlank();
  }
}
