package io.weblinkpilot.platform.bootstrap;

import io.weblinkpilot.ai.bootstrap.AiLinkMetadataBootstrapService;
import io.weblinkpilot.analytics.bootstrap.AnalyticsBootstrapService;
import io.weblinkpilot.auth.bootstrap.AuthBootstrapService;
import io.weblinkpilot.links.bootstrap.UrlBootstrapService;
import io.weblinkpilot.links.service.UrlStatisticsService;
import io.weblinkpilot.platform.PlatformProfiles;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile(PlatformProfiles.NOT_TEST)
public class BootstrapLinkRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapLinkRunner.class);
  private static final String DISABLED_ACCOUNT = "disabled";

  private final AuthBootstrapService authBootstrapService;
  private final UrlBootstrapService urlBootstrapService;
  private final AnalyticsBootstrapService analyticsBootstrapService;
  private final UrlStatisticsService urlStatisticsService;
  private final AiLinkMetadataBootstrapService aiLinkMetadataBootstrapService;

  public BootstrapLinkRunner(
      AuthBootstrapService authBootstrapService,
      UrlBootstrapService urlBootstrapService,
      AnalyticsBootstrapService analyticsBootstrapService,
      UrlStatisticsService urlStatisticsService,
      AiLinkMetadataBootstrapService aiLinkMetadataBootstrapService) {
    this.authBootstrapService = authBootstrapService;
    this.urlBootstrapService = urlBootstrapService;
    this.analyticsBootstrapService = analyticsBootstrapService;
    this.urlStatisticsService = urlStatisticsService;
    this.aiLinkMetadataBootstrapService = aiLinkMetadataBootstrapService;
  }

  @Override
  public void run(ApplicationArguments args) {
    AuthBootstrapService.BootstrapAccounts accounts = authBootstrapService.seedDefaultAccounts();
    urlBootstrapService.seedDefaultLinks(accounts.userUsername());
    aiLinkMetadataBootstrapService.seedDefaultMetadata();
    Map<String, Long> seededAnalyticsCounts = analyticsBootstrapService.seedDefaultAnalytics();
    urlStatisticsService.syncClickCounts(seededAnalyticsCounts);

    log.info(
        "bootstrap.links.seeded user={} admin={}",
        accounts.userUsername() == null ? DISABLED_ACCOUNT : accounts.userUsername(),
        accounts.adminUsername() == null ? DISABLED_ACCOUNT : accounts.adminUsername());
  }
}
