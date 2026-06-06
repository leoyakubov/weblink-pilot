package io.weblinkpilot.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.SocialLoginProvider;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.SocialIdentityRepository;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitHubOAuthServiceTest {

  @Mock private GitHubApiClient gitHubApiClient;
  @Mock private SocialIdentityRepository socialIdentityRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private UserAccountService userAccountService;
  @Mock private OAuthLoginService oauthLoginService;

  private GitHubOAuthService service;

  @BeforeEach
  void setUp() {
    AuthProperties authProperties = new AuthProperties();
    authProperties.setGithubClientId("github-client-id");
    authProperties.setGithubClientSecret("github-client-secret");
    authProperties.setGithubScope("read:user user:email");
    authProperties.setGithubLoginTicketTtlMinutes(10);
    service =
        new GitHubOAuthService(
            gitHubApiClient,
            socialIdentityRepository,
            userAccountRepository,
            userAccountService,
            oauthLoginService,
            authProperties);
  }

  @Test
  void completeLoginUsesVerifiedEmailAndIssuesTicket() {
    when(gitHubApiClient.exchangeCodeForAccessToken(
            "github-client-id", "github-client-secret", "code-1", "http://localhost/callback"))
        .thenReturn("access-token");
    when(gitHubApiClient.fetchProfile("access-token"))
        .thenReturn(new GitHubApiClient.GitHubProfile(1234L, "alice-github", "public@example.com"));
    when(gitHubApiClient.fetchEmails("access-token"))
        .thenReturn(
            List.of(
                new GitHubApiClient.GitHubEmail("alice@example.com", true, true),
                new GitHubApiClient.GitHubEmail("public@example.com", false, false)));

    UserAccount account = new UserAccount("gh1234", "hash", new Role("USER"), true, now());
    when(userAccountService.createSocialUser("gh1234", "alice@example.com")).thenReturn(account);
    when(oauthLoginService.issueLoginTicket(account)).thenReturn("login-ticket");

    String ticket = service.completeLogin("code-1", "http://localhost/callback");

    assertThat(ticket).isEqualTo("login-ticket");
    verify(userAccountService).createSocialUser("gh1234", "alice@example.com");
    ArgumentCaptor<SocialIdentity> identityCaptor = ArgumentCaptor.forClass(SocialIdentity.class);
    verify(socialIdentityRepository).save(identityCaptor.capture());
    assertThat(identityCaptor.getValue().getProvider()).isEqualTo(SocialLoginProvider.GITHUB);
    assertThat(identityCaptor.getValue().getProviderUserId()).isEqualTo("1234");
    assertThat(identityCaptor.getValue().getProviderLogin()).isEqualTo("alice-github");
    assertThat(identityCaptor.getValue().getUser()).isSameAs(account);
  }

  @Test
  void completeLoginWithoutVerifiedEmailDoesNotUseProfileEmailFallback() {
    when(gitHubApiClient.exchangeCodeForAccessToken(
            "github-client-id", "github-client-secret", "code-2", "http://localhost/callback"))
        .thenReturn("access-token");
    when(gitHubApiClient.fetchProfile("access-token"))
        .thenReturn(new GitHubApiClient.GitHubProfile(5678L, "bob-github", "public@example.com"));
    when(gitHubApiClient.fetchEmails("access-token")).thenReturn(List.of());

    UserAccount account = new UserAccount("gh5678", "hash", new Role("USER"), true, now());
    when(userAccountService.createSocialUser("gh5678", null)).thenReturn(account);
    when(oauthLoginService.issueLoginTicket(account)).thenReturn("login-ticket");

    String ticket = service.completeLogin("code-2", "http://localhost/callback");

    assertThat(ticket).isEqualTo("login-ticket");
    verify(userAccountService).createSocialUser("gh5678", null);
    ArgumentCaptor<SocialIdentity> identityCaptor = ArgumentCaptor.forClass(SocialIdentity.class);
    verify(socialIdentityRepository).save(identityCaptor.capture());
    assertThat(identityCaptor.getValue().getProviderLogin()).isEqualTo("bob-github");
    assertThat(identityCaptor.getValue().getUser()).isSameAs(account);
  }

  @Test
  void completeLoginReusesExistingIdentity() {
    when(gitHubApiClient.exchangeCodeForAccessToken(
            "github-client-id", "github-client-secret", "code-3", "http://localhost/callback"))
        .thenReturn("access-token");
    when(gitHubApiClient.fetchProfile("access-token"))
        .thenReturn(new GitHubApiClient.GitHubProfile(9012L, "carol-github", "public@example.com"));
    when(gitHubApiClient.fetchEmails("access-token"))
        .thenReturn(List.of(new GitHubApiClient.GitHubEmail("carol@example.com", true, true)));

    UserAccount account =
        new UserAccount("carol", "hash", null, new Role("USER"), true, now(), null);
    SocialIdentity identity =
        new SocialIdentity(
            SocialLoginProvider.GITHUB, "9012", "carol-github", account, now(), now());
    when(socialIdentityRepository.findByProviderAndProviderUserId(
            SocialLoginProvider.GITHUB, "9012"))
        .thenReturn(Optional.of(identity));
    when(oauthLoginService.issueLoginTicket(account)).thenReturn("login-ticket");

    String ticket = service.completeLogin("code-3", "http://localhost/callback");

    assertThat(ticket).isEqualTo("login-ticket");
    verify(userAccountService, never()).createSocialUser(any(), any());
    verify(userAccountRepository).save(account);
    verify(socialIdentityRepository).save(identity);
    assertThat(account.getEmail()).isEqualTo("carol@example.com");
    assertThat(account.isEmailVerified()).isTrue();
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
