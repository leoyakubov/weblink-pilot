package io.weblinkpilot.bootstrap;

import io.weblinkpilot.url.config.ShortLinkProperties;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.service.UrlCacheService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
  private final UrlCacheService cacheService;
  private final ShortLinkProperties shortLinkProperties;

  public ShortLinkCleanupJob(
      ShortLinkRepository shortLinkRepository,
      UrlCacheService cacheService,
      ShortLinkProperties shortLinkProperties) {
    this.shortLinkRepository = shortLinkRepository;
    this.cacheService = cacheService;
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
    List<ShortLink> expiredLinks = shortLinkRepository.findExpiredLinksBefore(cutoff);
    if (expiredLinks.isEmpty()) {
      log.info("url.link.cleanup.archived cutoff={} archived=0", cutoff);
      return;
    }

    OffsetDateTime archivedAt = OffsetDateTime.now(ZoneOffset.UTC);
    for (ShortLink link : expiredLinks) {
      link.markDeleted(archivedAt);
      cacheService.evict(link.getCode());
    }
    shortLinkRepository.saveAll(expiredLinks);
    log.info("url.link.cleanup.archived cutoff={} archived={}", cutoff, expiredLinks.size());
  }
}
