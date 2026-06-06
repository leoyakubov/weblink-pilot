package io.weblinkpilot.bootstrap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.service.UserAccountService;
import io.weblinkpilot.links.service.UrlBootstrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BootstrapLinkRunnerTest {

  @Mock private UserAccountService userAccountService;

  @Mock private UrlBootstrapService urlBootstrapService;

  private BootstrapLinkRunner runner;

  @BeforeEach
  void setUp() {
    runner = new BootstrapLinkRunner(userAccountService, urlBootstrapService);
  }

  @Test
  void seedsDefaultLinksFromBootstrapAccounts() {
    UserAccount admin =
        new UserAccount("admin", "hash", "admin@example.com", new Role("ADMIN"), true, null, null);
    UserAccount user =
        new UserAccount("user", "hash", "user@example.com", new Role("USER"), true, null, null);
    when(userAccountService.ensureBootstrapAdmin()).thenReturn(admin);
    when(userAccountService.ensureBootstrapUser()).thenReturn(user);

    runner.run(null);

    verify(userAccountService).ensureBootstrapAdmin();
    verify(userAccountService).ensureBootstrapUser();
    verify(urlBootstrapService).seedDefaultLinks("user", "admin");
  }
}
