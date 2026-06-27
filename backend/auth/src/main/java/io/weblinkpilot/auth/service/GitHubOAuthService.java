package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.SocialLoginProvider;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.repository.SocialIdentityRepository;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GitHubOAuthService {

  private static final String AUTHORIZATION_BASE_URL = "https://github.com/login/oauth/authorize";
  private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

  private final GitHubApiClient gitHubApiClient;
  private final SocialIdentityRepository socialIdentityRepository;
  private final UserAccountRepository userAccountRepository;
  private final UserAccountService userAccountService;
  private final OAuthLoginService oauthLoginService;
  private final String clientId;
  private final String clientSecret;
  private final String scope;
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
  private final SecureRandom secureRandom = new SecureRandom();

  public GitHubOAuthService(
      GitHubApiClient gitHubApiClient,
      SocialIdentityRepository socialIdentityRepository,
      UserAccountRepository userAccountRepository,
      UserAccountService userAccountService,
      OAuthLoginService oauthLoginService,
      AuthProperties authProperties) {
    this.gitHubApiClient = gitHubApiClient;
    this.socialIdentityRepository = socialIdentityRepository;
    this.userAccountRepository = userAccountRepository;
    this.userAccountService = userAccountService;
    this.oauthLoginService = oauthLoginService;
    this.clientId = authProperties.getGithubClientId();
    this.clientSecret = authProperties.getGithubClientSecret();
    this.scope = authProperties.getGithubScope();
  }

  public boolean isConfigured() {
    return clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }

  public String createStateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return encoder.encodeToString(bytes);
  }

  public String buildAuthorizationUrl(String redirectUri, String state) {
    ensureConfigured();
    return UriComponentsBuilder.fromUriString(AUTHORIZATION_BASE_URL)
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("scope", scope)
        .queryParam("state", state)
        .queryParam("allow_signup", "false")
        .build()
        .encode()
        .toUriString();
  }

  @Transactional
  public String completeLogin(String code, String redirectUri) {
    ensureConfigured();
    String accessToken =
        gitHubApiClient.exchangeCodeForAccessToken(clientId, clientSecret, code, redirectUri);
    GitHubApiClient.GitHubProfile profile = gitHubApiClient.fetchProfile(accessToken);
    String email = resolveVerifiedEmail(accessToken);
    UserAccount account = resolveAccount(profile, email);
    return oauthLoginService.issueLoginTicket(account);
  }

  private void ensureConfigured() {
    if (!isConfigured()) {
      throw new IllegalStateException("GitHub login is not configured");
    }
  }

  private String resolveVerifiedEmail(String accessToken) {
    List<GitHubApiClient.GitHubEmail> emails = gitHubApiClient.fetchEmails(accessToken);
    Optional<String> verifiedPrimary =
        emails.stream()
            .filter(GitHubApiClient.GitHubEmail::verified)
            .filter(GitHubApiClient.GitHubEmail::primary)
            .map(GitHubApiClient.GitHubEmail::email)
            .filter(email -> email != null && !email.isBlank())
            .findFirst();
    if (verifiedPrimary.isPresent()) {
      return verifiedPrimary.get().trim().toLowerCase(Locale.ROOT);
    }
    Optional<String> verifiedAny =
        emails.stream()
            .filter(GitHubApiClient.GitHubEmail::verified)
            .map(GitHubApiClient.GitHubEmail::email)
            .filter(email -> email != null && !email.isBlank())
            .findFirst();
    if (verifiedAny.isPresent()) {
      return verifiedAny.get().trim().toLowerCase(Locale.ROOT);
    }
    return null;
  }

  private UserAccount resolveAccount(GitHubApiClient.GitHubProfile profile, String email) {
    String providerUserId = Long.toString(profile.id());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    SocialIdentity identity =
        socialIdentityRepository
            .findByProviderAndProviderUserId(SocialLoginProvider.GITHUB, providerUserId)
            .orElse(null);
    if (identity != null) {
      UserAccount account = identity.getUser();
      if (!account.isEnabled()) {
        throw new AccountDisabledException();
      }
      if (email != null
          && !email.isBlank()
          && (account.getEmail() == null || account.getEmail().isBlank())) {
        account.setEmail(email);
        account.markEmailVerified(now);
        userAccountRepository.save(account);
      }
      identity.markLoggedIn(now);
      socialIdentityRepository.save(identity);
      log.info(
          "auth.github.login.reused username={} githubLogin={} githubUserId={}",
          account.getUsername(),
          identity.getProviderLogin(),
          providerUserId);
      return account;
    }

    UserAccount account = null;
    if (email != null && !email.isBlank()) {
      account = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    if (account != null) {
      if (!account.isEnabled()) {
        throw new AccountDisabledException();
      }
      if (account.getEmail() == null || account.getEmail().isBlank()) {
        account.setEmail(email);
        account.markEmailVerified(now);
        userAccountRepository.save(account);
      }
      log.info(
          "auth.github.account.linked username={} githubLogin={} githubUserId={} email={}",
          account.getUsername(),
          profile.login(),
          providerUserId,
          email);
    } else {
      String username = createUsername(profile);
      account = userAccountService.createSocialUser(username, email);
      log.info(
          "auth.github.account.created username={} githubLogin={} githubUserId={} emailVerifiedByGithub={}",
          account.getUsername(),
          profile.login(),
          providerUserId,
          email != null && !email.isBlank());
    }

    socialIdentityRepository.save(
        new SocialIdentity(
            SocialLoginProvider.GITHUB, providerUserId, profile.login(), account, now, now));
    return account;
  }

  private String createUsername(GitHubApiClient.GitHubProfile profile) {
    String base = normalizeGithubLogin(profile.login());
    if (base.isBlank()) {
      base = "gh" + profile.id();
    }
    String username = base;
    int suffix = 1;
    while (userAccountRepository.existsByUsername(username)) {
      username = base + suffix;
      suffix++;
    }
    return username;
  }

  private String normalizeGithubLogin(String login) {
    if (login == null) {
      return "";
    }
    return login
        .trim()
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9-]", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
  }
}
