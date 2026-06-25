package io.weblinkpilot.testsupport;

import io.weblinkpilot.analytics.service.AnalyticsBootstrapService;
import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.auth.service.RoleCatalogService;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
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
  private final AnalyticsBootstrapService analyticsBootstrapService;

  public TestBootstrapDataSeeder(
      UserAccountRepository userAccountRepository,
      ShortLinkRepository shortLinkRepository,
      PasswordEncoder passwordEncoder,
      RoleCatalogService roleCatalogService,
      AnalyticsBootstrapService analyticsBootstrapService) {
    this.userAccountRepository = userAccountRepository;
    this.shortLinkRepository = shortLinkRepository;
    this.passwordEncoder = passwordEncoder;
    this.roleCatalogService = roleCatalogService;
    this.analyticsBootstrapService = analyticsBootstrapService;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    ensureUser(BootstrapDefaults.ADMIN_USERNAME, BootstrapDefaults.ADMIN_PASSWORD, RoleNames.ADMIN);
    ensureUser(BootstrapDefaults.USER_USERNAME, BootstrapDefaults.USER_PASSWORD, RoleNames.USER);

    seedLink("spring-boot", "https://spring.io/projects/spring-boot", null, null, now);
    seedLink("vue-js", "https://vuejs.org/guide/introduction.html", null, null, now);
    seedLink(
        "postgres",
        "https://www.postgresql.org/about/",
        null,
        BootstrapDefaults.USER_USERNAME,
        now);
    seedLink(
        "redis",
        "https://redis.io/docs/latest/develop/",
        null,
        BootstrapDefaults.USER_USERNAME,
        now);

    analyticsBootstrapService.seedDefaultAnalytics();
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
