package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.domain.SocialIdentity;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.repository.SocialIdentityRepository;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.shared.contracts.AccountIdentityResponse;
import io.weblinkpilot.shared.contracts.AccountProfileResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountManagementService {

  private final UserAccountRepository userAccountRepository;
  private final SocialIdentityRepository socialIdentityRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final UserAccountService userAccountService;

  public AccountManagementService(
      UserAccountRepository userAccountRepository,
      SocialIdentityRepository socialIdentityRepository,
      PasswordEncoder passwordEncoder,
      RefreshTokenService refreshTokenService,
      UserAccountService userAccountService) {
    this.userAccountRepository = userAccountRepository;
    this.socialIdentityRepository = socialIdentityRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenService = refreshTokenService;
    this.userAccountService = userAccountService;
  }

  @Transactional(readOnly = true)
  public AccountProfileResponse profile(String username) {
    UserAccount account = userAccountService.getRequiredUser(username);
    List<AccountIdentityResponse> identities =
        socialIdentityRepository.findAllByUsername(account.getUsername()).stream()
            .map(this::toResponse)
            .toList();
    return new AccountProfileResponse(
        account.getUsername(),
        account.getRoleName(),
        account.getEmail(),
        account.isEmailVerified(),
        toText(account.getCreatedAt()),
        toText(account.getLastLoginAt()),
        identities);
  }

  @Transactional
  public void changePassword(String username, String currentPassword, String newPassword) {
    UserAccount account = userAccountService.getRequiredUser(username);
    if (currentPassword == null
        || currentPassword.isBlank()
        || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    userAccountService.validatePasswordPolicy(newPassword);
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    userAccountRepository.save(account);
    refreshTokenService.revokeAllForUser(account.getUsername());
  }

  private AccountIdentityResponse toResponse(SocialIdentity identity) {
    return new AccountIdentityResponse(identity.getProvider().name(), identity.getProviderLogin());
  }

  private String toText(OffsetDateTime value) {
    return value == null ? null : value.toString();
  }
}
