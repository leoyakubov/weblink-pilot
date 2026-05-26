package io.weblinkpilot.testsupport;

import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.auth.service.RoleCatalogService;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@Order(1)
public class TestBootstrapDataSeeder implements ApplicationRunner {

  private final UserAccountRepository userAccountRepository;
  private final ShortLinkRepository shortLinkRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleCatalogService roleCatalogService;

  public TestBootstrapDataSeeder(
      UserAccountRepository userAccountRepository,
      ShortLinkRepository shortLinkRepository,
      PasswordEncoder passwordEncoder,
      RoleCatalogService roleCatalogService) {
    this.userAccountRepository = userAccountRepository;
    this.shortLinkRepository = shortLinkRepository;
    this.passwordEncoder = passwordEncoder;
    this.roleCatalogService = roleCatalogService;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    ensureUser(BootstrapDefaults.ADMIN_USERNAME, BootstrapDefaults.ADMIN_PASSWORD, RoleNames.ADMIN);
    ensureUser(BootstrapDefaults.USER_USERNAME, BootstrapDefaults.USER_PASSWORD, RoleNames.USER);

    seedLink("welcome", "https://github.com/weblinkpilot/weblink-pilot", null, null, now);
    seedLink(
        "docs", "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs", null, null, now);
    seedLink(
        "user-home",
        "https://github.com/weblinkpilot/weblink-pilot/issues",
        null,
        BootstrapDefaults.USER_USERNAME,
        now);
    seedLink(
        "admin-home",
        "https://github.com/weblinkpilot/weblink-pilot/actions",
        null,
        BootstrapDefaults.ADMIN_USERNAME,
        now);
  }

  private void ensureUser(String username, String password, String roleName) {
    if (userAccountRepository.findByUsername(username).isPresent()) {
      return;
    }

    UserAccount account =
        new UserAccount(
            username,
            passwordEncoder.encode(password),
            username + "@weblinkpilot.local",
            roleCatalogService.getRequiredRole(roleName),
            true,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    userAccountRepository.save(account);
  }

  private void seedLink(
      String code,
      String originalUrl,
      String customAlias,
      String ownerUsername,
      OffsetDateTime createdAt) {
    if (shortLinkRepository.existsByCode(code)) {
      return;
    }
    if (customAlias != null && shortLinkRepository.existsByCustomAlias(customAlias)) {
      return;
    }

    ShortLink link = new ShortLink(code, originalUrl, customAlias, ownerUsername, createdAt, null);
    shortLinkRepository.save(link);
  }
}
