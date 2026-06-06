package io.weblinkpilot.bootstrap;

import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.service.ShortLinkCleanupService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ShortLinkCleanupJob {

  private final ShortLinkCleanupService cleanupService;
  private final ShortLinkProperties shortLinkProperties;

  public ShortLinkCleanupJob(
      ShortLinkCleanupService cleanupService, ShortLinkProperties shortLinkProperties) {
    this.cleanupService = cleanupService;
    this.shortLinkProperties = shortLinkProperties;
  }

  @Scheduled(cron = "${app.short-link.cleanup-cron:0 15 3 * * *}", zone = "UTC")
  public void purgeExpiredLinks() {
    cleanupService.purgeExpiredLinks(shortLinkProperties.getCleanupRetention());
  }
}
