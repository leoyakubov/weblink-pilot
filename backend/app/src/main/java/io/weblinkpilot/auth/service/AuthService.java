package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.AuthResponse;
import io.weblinkpilot.shared.contracts.RefreshTokenRequest;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountService userAccountService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.userAccountService = userAccountService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  @Transactional
  public AuthResponse register(AuthCredentialsRequest request) {
    UserAccount account = userAccountService.registerUser(request.username(), request.password());
    return issueSession(account);
  }

  @Transactional
  public AuthResponse login(AuthCredentialsRequest request) {
    UserAccount account = userAccountService.authenticate(request.username(), request.password());
    return issueSession(account);
  }

  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    RefreshTokenService.RotationResult rotation =
        refreshTokenService.rotateRefreshToken(request.refreshToken());
    return new AuthResponse(
        jwtService.issueToken(rotation.account().getUsername(), rotation.account().getRoleName()),
        rotation.refreshToken(),
        rotation.account().getUsername(),
        rotation.account().getRoleName());
  }

  @Transactional
  public void logout(RefreshTokenRequest request) {
    refreshTokenService.revokeRefreshToken(request.refreshToken());
  }

  @Transactional(readOnly = true)
  public UserProfileResponse profile(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Authentication required");
    }
    return userAccountService.profile(authentication.getName());
  }

  private AuthResponse issueSession(UserAccount account) {
    String refreshToken = refreshTokenService.issueRefreshToken(account);
    return new AuthResponse(
        jwtService.issueToken(account.getUsername(), account.getRoleName()),
        refreshToken,
        account.getUsername(),
        account.getRoleName());
  }
}
