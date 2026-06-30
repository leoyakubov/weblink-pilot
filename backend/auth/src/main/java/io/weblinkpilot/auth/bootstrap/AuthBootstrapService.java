package io.weblinkpilot.auth.bootstrap;

import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.service.UserAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthBootstrapService {

  private final UserAccountService userAccountService;

  public AuthBootstrapService(UserAccountService userAccountService) {
    this.userAccountService = userAccountService;
  }

  @Transactional
  public BootstrapAccounts seedDefaultAccounts() {
    UserAccount admin = userAccountService.ensureBootstrapAdmin();
    UserAccount user = userAccountService.ensureBootstrapUser();
    return new BootstrapAccounts(admin.getUsername(), user.getUsername());
  }

  public record BootstrapAccounts(String adminUsername, String userUsername) {}
}
