package io.weblinkpilot.auth.service;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.exception.InvalidCredentialsException;
import io.weblinkpilot.auth.mapper.AuthResponseMapper;
import io.weblinkpilot.auth.repository.SocialIdentityRepository;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.auth.token.RefreshTokenService;
import io.weblinkpilot.shared.api.auth.AccountProfileResponse;
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
  private final AuthResponseMapper responseMapper;

  public AccountManagementService(
      UserAccountRepository userAccountRepository,
      SocialIdentityRepository socialIdentityRepository,
      PasswordEncoder passwordEncoder,
      RefreshTokenService refreshTokenService,
      UserAccountService userAccountService,
      AuthResponseMapper responseMapper) {
    this.userAccountRepository = userAccountRepository;
    this.socialIdentityRepository = socialIdentityRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenService = refreshTokenService;
    this.userAccountService = userAccountService;
    this.responseMapper = responseMapper;
  }

  @Transactional(readOnly = true)
  public AccountProfileResponse profile(String username) {
    UserAccount account = userAccountService.getRequiredUser(username);
    var identities =
        socialIdentityRepository.findAllByUsername(account.getUsername()).stream()
            .map(responseMapper::toIdentity)
            .toList();
    return responseMapper.toProfile(account, identities);
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
}
