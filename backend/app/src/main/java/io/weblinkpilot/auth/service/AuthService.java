package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.shared.contracts.AuthCredentialsRequest;
import io.weblinkpilot.shared.contracts.UserProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  public record AuthSession(String token, String refreshToken, String username, String role) {}

  private final UserAccountService userAccountService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordResetService passwordResetService;

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      PasswordResetService passwordResetService) {
    this.userAccountService = userAccountService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.passwordResetService = passwordResetService;
  }

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this(userAccountService, jwtService, refreshTokenService, null);
  }

  @Transactional
  public AuthSession register(AuthCredentialsRequest request) {
    UserAccount account =
        userAccountService.registerUser(request.username(), request.password(), request.email());
    return issueSession(account);
  }

  @Transactional
  public AuthSession login(AuthCredentialsRequest request) {
    UserAccount account = userAccountService.authenticate(request.username(), request.password());
    return issueSession(account);
  }

  @Transactional
  public AuthSession refresh(String refreshToken) {
    RefreshTokenService.RotationResult rotation =
        refreshTokenService.rotateRefreshToken(refreshToken);
    return new AuthSession(
        jwtService.issueToken(rotation.username(), rotation.role()),
        rotation.refreshToken(),
        rotation.username(),
        rotation.role());
  }

  @Transactional
  public void logout(String refreshToken) {
    refreshTokenService.revokeRefreshToken(refreshToken);
  }

  @Transactional
  public void requestPasswordReset(String email) {
    if (passwordResetService == null) {
      throw new IllegalStateException("Password reset service is not configured");
    }
    passwordResetService.requestPasswordReset(email);
  }

  @Transactional
  public void confirmPasswordReset(String token, String password) {
    if (passwordResetService == null) {
      throw new IllegalStateException("Password reset service is not configured");
    }
    passwordResetService.confirmPasswordReset(token, password);
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

  private AuthSession issueSession(UserAccount account) {
    String refreshToken = refreshTokenService.issueRefreshToken(account);
    return new AuthSession(
        jwtService.issueToken(account.getUsername(), account.getRoleName()),
        refreshToken,
        account.getUsername(),
        account.getRoleName());
  }
}
