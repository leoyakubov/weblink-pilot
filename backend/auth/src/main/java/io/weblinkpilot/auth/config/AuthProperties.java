package io.weblinkpilot.auth.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Spring configuration properties expose nested mutable beans for binding.")
public class AuthProperties {

  @Valid private Jwt jwt = new Jwt();
  @Valid private RefreshToken refreshToken = new RefreshToken();
  @Valid private AccountActions accountActions = new AccountActions();
  @Valid private RefreshCookie refreshCookie = new RefreshCookie();
  @Valid private Frontend frontend = new Frontend();
  @Valid private GitHub github = new GitHub();
  @Valid private Mail mail = new Mail();
  @Valid private Bootstrap bootstrap = new Bootstrap();

  public Jwt getJwt() {
    return jwt;
  }

  public void setJwt(Jwt jwt) {
    this.jwt = jwt == null ? new Jwt() : jwt;
  }

  public RefreshToken getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(RefreshToken refreshToken) {
    this.refreshToken = refreshToken == null ? new RefreshToken() : refreshToken;
  }

  public AccountActions getAccountActions() {
    return accountActions;
  }

  public void setAccountActions(AccountActions accountActions) {
    this.accountActions = accountActions == null ? new AccountActions() : accountActions;
  }

  public RefreshCookie getRefreshCookie() {
    return refreshCookie;
  }

  public void setRefreshCookie(RefreshCookie refreshCookie) {
    this.refreshCookie = refreshCookie == null ? new RefreshCookie() : refreshCookie;
  }

  public Frontend getFrontend() {
    return frontend;
  }

  public void setFrontend(Frontend frontend) {
    this.frontend = frontend == null ? new Frontend() : frontend;
  }

  public GitHub getGithub() {
    return github;
  }

  public void setGithub(GitHub github) {
    this.github = github == null ? new GitHub() : github;
  }

  public Mail getMail() {
    return mail;
  }

  public void setMail(Mail mail) {
    this.mail = mail == null ? new Mail() : mail;
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap == null ? new Bootstrap() : bootstrap;
  }

  public String getIssuer() {
    return jwt.getIssuer();
  }

  public void setIssuer(String issuer) {
    jwt.setIssuer(issuer);
  }

  public String getJwtSecret() {
    return jwt.getSecret();
  }

  public void setJwtSecret(String jwtSecret) {
    jwt.setSecret(jwtSecret);
  }

  public long getTokenTtlMinutes() {
    return jwt.getAccessTokenTtlMinutes();
  }

  public void setTokenTtlMinutes(long tokenTtlMinutes) {
    jwt.setAccessTokenTtlMinutes(tokenTtlMinutes);
  }

  public long getRefreshTokenTtlDays() {
    return refreshToken.getTtlDays();
  }

  public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
    refreshToken.setTtlDays(refreshTokenTtlDays);
  }

  public long getAccountActionTokenTtlHours() {
    return accountActions.getTokenTtlHours();
  }

  public void setAccountActionTokenTtlHours(long accountActionTokenTtlHours) {
    accountActions.setTokenTtlHours(accountActionTokenTtlHours);
  }

  public long getAccountActionRequestCooldownSeconds() {
    return accountActions.getRequestCooldownSeconds();
  }

  public void setAccountActionRequestCooldownSeconds(long accountActionRequestCooldownSeconds) {
    accountActions.setRequestCooldownSeconds(accountActionRequestCooldownSeconds);
  }

  public long getGithubLoginTicketTtlMinutes() {
    return github.getLoginTicketTtlMinutes();
  }

  public void setGithubLoginTicketTtlMinutes(long githubLoginTicketTtlMinutes) {
    github.setLoginTicketTtlMinutes(githubLoginTicketTtlMinutes);
  }

  public String getRefreshCookieName() {
    return refreshCookie.getName();
  }

  public void setRefreshCookieName(String refreshCookieName) {
    refreshCookie.setName(refreshCookieName);
  }

  public String getRefreshCookiePath() {
    return refreshCookie.getPath();
  }

  public void setRefreshCookiePath(String refreshCookiePath) {
    refreshCookie.setPath(refreshCookiePath);
  }

  public String getFrontendBaseUrl() {
    return frontend.getBaseUrl();
  }

  public void setFrontendBaseUrl(String frontendBaseUrl) {
    frontend.setBaseUrl(frontendBaseUrl);
  }

  public String getGithubClientId() {
    return github.getClientId();
  }

  public void setGithubClientId(String githubClientId) {
    github.setClientId(githubClientId);
  }

  public String getGithubClientSecret() {
    return github.getClientSecret();
  }

  public void setGithubClientSecret(String githubClientSecret) {
    github.setClientSecret(githubClientSecret);
  }

  public String getGithubScope() {
    return github.getScope();
  }

  public void setGithubScope(String githubScope) {
    github.setScope(githubScope);
  }

  public String getGithubStateCookieName() {
    return github.getStateCookie().getName();
  }

  public void setGithubStateCookieName(String githubStateCookieName) {
    github.getStateCookie().setName(githubStateCookieName);
  }

  public String getGithubStateCookiePath() {
    return github.getStateCookie().getPath();
  }

  public void setGithubStateCookiePath(String githubStateCookiePath) {
    github.getStateCookie().setPath(githubStateCookiePath);
  }

  public String getRefreshCookieSameSite() {
    return refreshCookie.getSameSite();
  }

  public void setRefreshCookieSameSite(String refreshCookieSameSite) {
    refreshCookie.setSameSite(refreshCookieSameSite);
  }

  public boolean isRefreshCookieSecure() {
    return refreshCookie.isSecure();
  }

  public void setRefreshCookieSecure(boolean refreshCookieSecure) {
    refreshCookie.setSecure(refreshCookieSecure);
  }

  public String getBootstrapAdminUsername() {
    return bootstrap.getAdmin().getUsername();
  }

  public void setBootstrapAdminUsername(String bootstrapAdminUsername) {
    bootstrap.getAdmin().setUsername(bootstrapAdminUsername);
  }

  public String getBootstrapAdminPassword() {
    return bootstrap.getAdmin().getPassword();
  }

  public void setBootstrapAdminPassword(String bootstrapAdminPassword) {
    bootstrap.getAdmin().setPassword(bootstrapAdminPassword);
  }

  public String getBootstrapAdminRole() {
    return bootstrap.getAdmin().getRole();
  }

  public void setBootstrapAdminRole(String bootstrapAdminRole) {
    bootstrap.getAdmin().setRole(bootstrapAdminRole);
  }

  public String getBootstrapAdminEmail() {
    return bootstrap.getAdmin().getEmail();
  }

  public void setBootstrapAdminEmail(String bootstrapAdminEmail) {
    bootstrap.getAdmin().setEmail(bootstrapAdminEmail);
  }

  public String getBootstrapUserUsername() {
    return bootstrap.getUser().getUsername();
  }

  public void setBootstrapUserUsername(String bootstrapUserUsername) {
    bootstrap.getUser().setUsername(bootstrapUserUsername);
  }

  public String getBootstrapUserPassword() {
    return bootstrap.getUser().getPassword();
  }

  public void setBootstrapUserPassword(String bootstrapUserPassword) {
    bootstrap.getUser().setPassword(bootstrapUserPassword);
  }

  public String getBootstrapUserRole() {
    return bootstrap.getUser().getRole();
  }

  public void setBootstrapUserRole(String bootstrapUserRole) {
    bootstrap.getUser().setRole(bootstrapUserRole);
  }

  public String getBootstrapUserEmail() {
    return bootstrap.getUser().getEmail();
  }

  public void setBootstrapUserEmail(String bootstrapUserEmail) {
    bootstrap.getUser().setEmail(bootstrapUserEmail);
  }

  public String getMailDeliveryMode() {
    return mail.getDeliveryMode().name();
  }

  public void setMailDeliveryMode(String mailDeliveryMode) {
    mail.setDeliveryMode(MailDeliveryMode.valueOf(mailDeliveryMode.toUpperCase(Locale.ROOT)));
  }

  public boolean isDemoMailboxEnabled() {
    return MailDeliveryMode.DEMO_PREVIEW == mail.getDeliveryMode();
  }

  public boolean isGithubConfigured() {
    return github.getClientId() != null
        && !github.getClientId().isBlank()
        && github.getClientSecret() != null
        && !github.getClientSecret().isBlank();
  }

  public static class Jwt {

    @NotBlank private String issuer;
    @NotBlank private String secret;

    @Min(15)
    private long accessTokenTtlMinutes;

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      this.issuer = issuer;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public long getAccessTokenTtlMinutes() {
      return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
      this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }
  }

  public static class RefreshToken {

    @Min(1)
    private long ttlDays;

    public long getTtlDays() {
      return ttlDays;
    }

    public void setTtlDays(long ttlDays) {
      this.ttlDays = ttlDays;
    }
  }

  public static class AccountActions {

    @Min(1)
    private long tokenTtlHours;

    @Min(1)
    private long requestCooldownSeconds = 30;

    public long getTokenTtlHours() {
      return tokenTtlHours;
    }

    public void setTokenTtlHours(long tokenTtlHours) {
      this.tokenTtlHours = tokenTtlHours;
    }

    public long getRequestCooldownSeconds() {
      return requestCooldownSeconds;
    }

    public void setRequestCooldownSeconds(long requestCooldownSeconds) {
      this.requestCooldownSeconds = requestCooldownSeconds;
    }
  }

  public static class RefreshCookie {

    @NotBlank private String name;
    @NotBlank private String path;

    @Pattern(regexp = "(?i)STRICT|LAX|NONE")
    private String sameSite;

    private boolean secure;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getSameSite() {
      return sameSite;
    }

    public void setSameSite(String sameSite) {
      this.sameSite = sameSite;
    }

    public boolean isSecure() {
      return secure;
    }

    public void setSecure(boolean secure) {
      this.secure = secure;
    }
  }

  public static class Frontend {

    @NotBlank private String baseUrl;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }
  }

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "Spring configuration properties expose nested mutable beans for binding.")
  public static class GitHub {

    private String clientId;
    private String clientSecret;
    @NotBlank private String scope;
    @NotNull private URI authorizationUrl;
    @NotNull private URI tokenUrl;
    @NotNull private URI apiBaseUrl;
    @NotBlank private String apiVersion;
    @NotBlank private String userAgent;
    @NotNull private Duration timeout;
    private boolean allowSignup;

    @Min(1)
    private long loginTicketTtlMinutes;

    @Min(16)
    private int stateEntropyBytes = 32;

    @Valid private Cookie stateCookie = new Cookie();

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public String getScope() {
      return scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }

    public URI getAuthorizationUrl() {
      return authorizationUrl;
    }

    public void setAuthorizationUrl(URI authorizationUrl) {
      this.authorizationUrl = authorizationUrl;
    }

    public URI getTokenUrl() {
      return tokenUrl;
    }

    public void setTokenUrl(URI tokenUrl) {
      this.tokenUrl = tokenUrl;
    }

    public URI getApiBaseUrl() {
      return apiBaseUrl;
    }

    public void setApiBaseUrl(URI apiBaseUrl) {
      this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiVersion() {
      return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    public boolean isAllowSignup() {
      return allowSignup;
    }

    public void setAllowSignup(boolean allowSignup) {
      this.allowSignup = allowSignup;
    }

    public long getLoginTicketTtlMinutes() {
      return loginTicketTtlMinutes;
    }

    public void setLoginTicketTtlMinutes(long loginTicketTtlMinutes) {
      this.loginTicketTtlMinutes = loginTicketTtlMinutes;
    }

    public int getStateEntropyBytes() {
      return stateEntropyBytes;
    }

    public void setStateEntropyBytes(int stateEntropyBytes) {
      this.stateEntropyBytes = stateEntropyBytes;
    }

    public Cookie getStateCookie() {
      return stateCookie;
    }

    public void setStateCookie(Cookie stateCookie) {
      this.stateCookie = stateCookie == null ? new Cookie() : stateCookie;
    }
  }

  public static class Cookie {

    @NotBlank private String name;
    @NotBlank private String path;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }

  public static class Mail {

    @NotNull private MailDeliveryMode deliveryMode = MailDeliveryMode.SMTP;

    public MailDeliveryMode getDeliveryMode() {
      return deliveryMode;
    }

    public void setDeliveryMode(MailDeliveryMode deliveryMode) {
      this.deliveryMode = deliveryMode == null ? MailDeliveryMode.SMTP : deliveryMode;
    }
  }

  public static class Bootstrap {

    @Valid private Account admin = new Account();
    @Valid private Account user = new Account();

    public Account getAdmin() {
      return admin;
    }

    public void setAdmin(Account admin) {
      this.admin = admin == null ? new Account() : admin;
    }

    public Account getUser() {
      return user;
    }

    public void setUser(Account user) {
      this.user = user == null ? new Account() : user;
    }
  }

  public static class Account {

    private String username;
    private String password;
    private String role;
    private String email;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
