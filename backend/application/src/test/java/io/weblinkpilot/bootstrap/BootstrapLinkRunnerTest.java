package io.weblinkpilot.bootstrap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.ai.service.AiLinkMetadataService;
import io.weblinkpilot.analytics.service.AnalyticsBootstrapService;
import io.weblinkpilot.auth.domain.Role;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.service.UserAccountService;
import io.weblinkpilot.links.service.UrlBootstrapService;
import io.weblinkpilot.links.service.UrlStatisticsService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BootstrapLinkRunnerTest {

  @Mock private UserAccountService userAccountService;

  @Mock private UrlBootstrapService urlBootstrapService;

  @Mock private AnalyticsBootstrapService analyticsBootstrapService;

  @Mock private UrlStatisticsService urlStatisticsService;

  @Mock private AiLinkMetadataService aiLinkMetadataService;

  private BootstrapLinkRunner runner;

  @BeforeEach
  void setUp() {
    runner =
        new BootstrapLinkRunner(
            userAccountService,
            urlBootstrapService,
            analyticsBootstrapService,
            urlStatisticsService,
            aiLinkMetadataService);
  }

  @Test
  void seedsDefaultLinksFromBootstrapAccounts() {
    UserAccount admin =
        new UserAccount("admin", "hash", "admin@example.com", new Role("ADMIN"), true, null, null);
    UserAccount user =
        new UserAccount("user", "hash", "user@example.com", new Role("USER"), true, null, null);
    when(userAccountService.ensureBootstrapAdmin()).thenReturn(admin);
    when(userAccountService.ensureBootstrapUser()).thenReturn(user);
    Map<String, Long> seededCounts = Map.of("redis", 6L);
    when(analyticsBootstrapService.seedDefaultAnalytics()).thenReturn(seededCounts);

    runner.run(null);

    verify(userAccountService).ensureBootstrapAdmin();
    verify(userAccountService).ensureBootstrapUser();
    verify(urlBootstrapService).seedDefaultLinks("user");
    verify(aiLinkMetadataService).seedDefaultMetadata();
    verify(analyticsBootstrapService).seedDefaultAnalytics();
    verify(urlStatisticsService).syncClickCounts(seededCounts);
  }
}
