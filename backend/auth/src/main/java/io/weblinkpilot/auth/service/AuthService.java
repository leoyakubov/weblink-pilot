package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.config.AuthProperties;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.session.AuthSession;
import io.weblinkpilot.auth.token.JwtService;
import io.weblinkpilot.auth.token.RefreshTokenService;
import io.weblinkpilot.shared.api.auth.AuthCredentialsRequest;
import io.weblinkpilot.shared.api.auth.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountService userAccountService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordResetService passwordResetService;
  private final EmailVerificationService emailVerificationService;
  private final boolean demoMailboxEnabled;

  @Autowired
  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      PasswordResetService passwordResetService,
      EmailVerificationService emailVerificationService,
      AuthProperties authProperties) {
    this.userAccountService = userAccountService;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.passwordResetService = passwordResetService;
    this.emailVerificationService = emailVerificationService;
    this.demoMailboxEnabled = authProperties.isDemoMailboxEnabled();
  }

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      PasswordResetService passwordResetService,
      EmailVerificationService emailVerificationService) {
    this(
        userAccountService,
        jwtService,
        refreshTokenService,
        passwordResetService,
        emailVerificationService,
        new AuthProperties());
  }

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this(userAccountService, jwtService, refreshTokenService, null, null);
  }

  public AuthService(
      UserAccountService userAccountService,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      PasswordResetService passwordResetService) {
    this(userAccountService, jwtService, refreshTokenService, passwordResetService, null);
  }

  @Transactional
  public String register(AuthCredentialsRequest request) {
    UserAccount account =
        userAccountService.registerUser(request.username(), request.password(), request.email());
    if (emailVerificationService != null && account.getEmail() != null) {
      return emailVerificationService.requestEmailVerification(account.getEmail());
    }
    return null;
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
  public String requestPasswordReset(String email) {
    if (passwordResetService == null) {
      throw new IllegalStateException("Password reset service is not configured");
    }
    return passwordResetService.requestPasswordReset(email);
  }

  @Transactional
  public void confirmPasswordReset(String token, String password) {
    if (passwordResetService == null) {
      throw new IllegalStateException("Password reset service is not configured");
    }
    passwordResetService.confirmPasswordReset(token, password);
  }

  @Transactional
  public String requestEmailVerification(String email) {
    if (emailVerificationService == null) {
      throw new IllegalStateException("Email verification service is not configured");
    }
    return emailVerificationService.requestEmailVerification(email);
  }

  @Transactional
  public void confirmEmailVerification(String token) {
    if (emailVerificationService == null) {
      throw new IllegalStateException("Email verification service is not configured");
    }
    emailVerificationService.confirmEmailVerification(token);
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

  public boolean isDemoMailboxEnabled() {
    return demoMailboxEnabled;
  }
}
