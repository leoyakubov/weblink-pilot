package io.weblinkpilot.platform.bootstrap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.ai.bootstrap.AiLinkMetadataBootstrapService;
import io.weblinkpilot.analytics.bootstrap.AnalyticsBootstrapService;
import io.weblinkpilot.auth.bootstrap.AuthBootstrapService;
import io.weblinkpilot.links.bootstrap.UrlBootstrapService;
import io.weblinkpilot.links.service.UrlStatisticsService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BootstrapLinkRunnerTest {

  @Mock private AuthBootstrapService authBootstrapService;

  @Mock private UrlBootstrapService urlBootstrapService;

  @Mock private AnalyticsBootstrapService analyticsBootstrapService;

  @Mock private UrlStatisticsService urlStatisticsService;

  @Mock private AiLinkMetadataBootstrapService aiLinkMetadataBootstrapService;

  private BootstrapLinkRunner runner;

  @BeforeEach
  void setUp() {
    runner =
        new BootstrapLinkRunner(
            authBootstrapService,
            urlBootstrapService,
            analyticsBootstrapService,
            urlStatisticsService,
            aiLinkMetadataBootstrapService);
  }

  @Test
  void seedsDefaultLinksFromBootstrapAccounts() {
    when(authBootstrapService.seedDefaultAccounts())
        .thenReturn(new AuthBootstrapService.BootstrapAccounts("admin", "user"));
    Map<String, Long> seededCounts = Map.of("redis", 6L);
    when(analyticsBootstrapService.seedDefaultAnalytics()).thenReturn(seededCounts);

    runner.run(null);

    verify(authBootstrapService).seedDefaultAccounts();
    verify(urlBootstrapService).seedDefaultLinks("user");
    verify(aiLinkMetadataBootstrapService).seedDefaultMetadata();
    verify(analyticsBootstrapService).seedDefaultAnalytics();
    verify(urlStatisticsService).syncClickCounts(seededCounts);
  }
}
