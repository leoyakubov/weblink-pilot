package io.weblinkpilot.links.service;

import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkCleanupService {

  private static final Logger log = LoggerFactory.getLogger(ShortLinkCleanupService.class);

  private final ShortLinkRepository shortLinkRepository;
  private final UrlCacheService cacheService;

  public ShortLinkCleanupService(
      ShortLinkRepository shortLinkRepository, UrlCacheService cacheService) {
    this.shortLinkRepository = shortLinkRepository;
    this.cacheService = cacheService;
  }

  @Transactional
  public void purgeExpiredLinks(Duration cleanupRetention) {
    if (cleanupRetention == null || cleanupRetention.isNegative()) {
      return;
    }

    OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minus(cleanupRetention);
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
