package io.weblinkpilot.auth.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.AccountActionTokenType;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.AccountDisabledException;
import io.weblinkpilot.auth.exception.InvalidAccountActionTokenException;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this service.")
public class OAuthLoginService {

  public record AuthSession(String token, String refreshToken, String username, String role) {}

  private final AccountActionTokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final UserAccountRepository userAccountRepository;
  private final long loginTicketTtlMinutes;

  public OAuthLoginService(
      AccountActionTokenService tokenService,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      UserAccountRepository userAccountRepository,
      AuthProperties authProperties) {
    this.tokenService = tokenService;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.userAccountRepository = userAccountRepository;
    this.loginTicketTtlMinutes = authProperties.getGithubLoginTicketTtlMinutes();
  }

  @Transactional
  public String issueLoginTicket(UserAccount account) {
    return tokenService.issueToken(
        account, AccountActionTokenType.OAUTH_LOGIN, Duration.ofMinutes(loginTicketTtlMinutes));
  }

  @Transactional
  public AuthSession completeLogin(String ticket) {
    UserAccount account =
        tokenService
            .consumeToken(ticket, AccountActionTokenType.OAUTH_LOGIN)
            .orElseThrow(InvalidAccountActionTokenException::new)
            .getUser();
    if (!account.isEnabled()) {
      throw new AccountDisabledException();
    }

    account.markLoggedIn(OffsetDateTime.now(ZoneOffset.UTC));
    userAccountRepository.save(account);
    String refreshToken = refreshTokenService.issueRefreshToken(account);
    return new AuthSession(
        jwtService.issueToken(account.getUsername(), account.getRoleName()),
        refreshToken,
        account.getUsername(),
        account.getRoleName());
  }
}
