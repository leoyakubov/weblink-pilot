package io.weblinkpilot.bootstrap;

import io.weblinkpilot.ai.service.AiLinkMetadataService;
import io.weblinkpilot.analytics.service.AnalyticsBootstrapService;
import io.weblinkpilot.auth.domain.UserAccount;
import io.weblinkpilot.auth.service.UserAccountService;
import io.weblinkpilot.links.service.UrlBootstrapService;
import io.weblinkpilot.links.service.UrlStatisticsService;
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
@Profile("!test")
public class BootstrapLinkRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapLinkRunner.class);

  private final UserAccountService userAccountService;
  private final UrlBootstrapService urlBootstrapService;
  private final AnalyticsBootstrapService analyticsBootstrapService;
  private final UrlStatisticsService urlStatisticsService;
  private final AiLinkMetadataService aiLinkMetadataService;

  public BootstrapLinkRunner(
      UserAccountService userAccountService,
      UrlBootstrapService urlBootstrapService,
      AnalyticsBootstrapService analyticsBootstrapService,
      UrlStatisticsService urlStatisticsService,
      AiLinkMetadataService aiLinkMetadataService) {
    this.userAccountService = userAccountService;
    this.urlBootstrapService = urlBootstrapService;
    this.analyticsBootstrapService = analyticsBootstrapService;
    this.urlStatisticsService = urlStatisticsService;
    this.aiLinkMetadataService = aiLinkMetadataService;
  }

  @Override
  public void run(ApplicationArguments args) {
    UserAccount admin = userAccountService.ensureBootstrapAdmin();
    UserAccount user = userAccountService.ensureBootstrapUser();
    urlBootstrapService.seedDefaultLinks(user == null ? null : user.getUsername());
    aiLinkMetadataService.seedDefaultMetadata();
    Map<String, Long> seededAnalyticsCounts = analyticsBootstrapService.seedDefaultAnalytics();
    urlStatisticsService.syncClickCounts(seededAnalyticsCounts);

    log.info(
        "bootstrap.links.seeded user={} admin={}",
        user == null ? "disabled" : user.getUsername(),
        admin == null ? "disabled" : admin.getUsername());
  }
}
