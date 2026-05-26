package io.weblinkpilot.bootstrap;

import io.weblinkpilot.url.config.ShortLinkProperties;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class ShortLinkCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(ShortLinkCleanupJob.class);

  private final ShortLinkRepository shortLinkRepository;
  private final ShortLinkProperties shortLinkProperties;

  public ShortLinkCleanupJob(
      ShortLinkRepository shortLinkRepository, ShortLinkProperties shortLinkProperties) {
    this.shortLinkRepository = shortLinkRepository;
    this.shortLinkProperties = shortLinkProperties;
  }

  @Scheduled(cron = "${app.short-link.cleanup-cron:0 15 3 * * *}", zone = "UTC")
  @Transactional
  public void purgeExpiredLinks() {
    if (shortLinkProperties.getCleanupRetention() == null
        || shortLinkProperties.getCleanupRetention().isNegative()) {
      return;
    }

    OffsetDateTime cutoff =
        OffsetDateTime.now(ZoneOffset.UTC).minus(shortLinkProperties.getCleanupRetention());
    int deleted = shortLinkRepository.deleteExpiredLinksBefore(cutoff);
    log.info("url.link.cleanup.purged cutoff={} deleted={}", cutoff, deleted);
  }
}
