package io.weblinkpilot.bootstrap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.service.ShortLinkCleanupService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed dependencies are intentionally retained by this job.")
public class ShortLinkCleanupJob {

  private final ShortLinkCleanupService cleanupService;
  private final ShortLinkProperties shortLinkProperties;

  public ShortLinkCleanupJob(
      ShortLinkCleanupService cleanupService, ShortLinkProperties shortLinkProperties) {
    this.cleanupService = cleanupService;
    this.shortLinkProperties = shortLinkProperties;
  }

  @Scheduled(cron = "#{@shortLinkProperties.cleanupCron}", zone = "UTC")
  public void purgeExpiredLinks() {
    cleanupService.purgeExpiredLinks(shortLinkProperties.getCleanupRetention());
  }
}
