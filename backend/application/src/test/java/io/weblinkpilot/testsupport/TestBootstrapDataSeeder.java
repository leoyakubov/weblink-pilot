package io.weblinkpilot.testsupport;

import io.weblinkpilot.analytics.bootstrap.AnalyticsBootstrapService;
import io.weblinkpilot.auth.config.BootstrapDefaults;
import io.weblinkpilot.auth.config.RoleNames;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.repository.UserAccountRepository;
import io.weblinkpilot.auth.service.RoleCatalogService;
import io.weblinkpilot.links.bootstrap.UrlBootstrapService;
import io.weblinkpilot.links.service.UrlStatisticsService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
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
  private final PasswordEncoder passwordEncoder;
  private final RoleCatalogService roleCatalogService;
  private final UrlBootstrapService urlBootstrapService;
  private final AnalyticsBootstrapService analyticsBootstrapService;
  private final UrlStatisticsService urlStatisticsService;

  public TestBootstrapDataSeeder(
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      RoleCatalogService roleCatalogService,
      UrlBootstrapService urlBootstrapService,
      AnalyticsBootstrapService analyticsBootstrapService,
      UrlStatisticsService urlStatisticsService) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.roleCatalogService = roleCatalogService;
    this.urlBootstrapService = urlBootstrapService;
    this.analyticsBootstrapService = analyticsBootstrapService;
    this.urlStatisticsService = urlStatisticsService;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ensureUser(BootstrapDefaults.ADMIN_USERNAME, BootstrapDefaults.ADMIN_PASSWORD, RoleNames.ADMIN);
    ensureUser(BootstrapDefaults.USER_USERNAME, BootstrapDefaults.USER_PASSWORD, RoleNames.USER);

    urlBootstrapService.seedDefaultLinks(BootstrapDefaults.USER_USERNAME);
    Map<String, Long> seededAnalyticsCounts = analyticsBootstrapService.seedDefaultAnalytics();
    urlStatisticsService.syncClickCounts(seededAnalyticsCounts);
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
}
