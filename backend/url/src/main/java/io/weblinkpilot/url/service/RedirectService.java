package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.UrlExpiredException;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.web.RedirectRequestContext;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RedirectService {

  private static final Logger log = LoggerFactory.getLogger(RedirectService.class);

  private final ShortLinkRepository repository;
  private final UrlCacheService cacheService;
  private final LinkPublisher linkPublisher;

  public RedirectService(
      ShortLinkRepository repository, UrlCacheService cacheService, LinkPublisher linkPublisher) {
    this.repository = repository;
    this.cacheService = cacheService;
    this.linkPublisher = linkPublisher;
  }

  @Transactional
  public String resolveTarget(String code, RedirectRequestContext context) {
    return resolveTarget(code, context, LinkTrackingSource.REDIRECT);
  }

  @Transactional
  public String resolveTarget(
      String code, RedirectRequestContext context, LinkTrackingSource source) {
    ShortLinkSnapshot snapshot = cacheService.findByCode(code);
    if (snapshot == null) {
      log.warn("url.link.redirect.miss code={}", code);
      throw new UrlNotFoundException(code);
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (snapshot.deletedAt() != null
        || (snapshot.expiresAt() != null && !snapshot.expiresAt().isAfter(now))) {
      log.warn("url.link.redirect.expired code={} expiredAt={}", code, snapshot.expiresAt());
      throw new UrlExpiredException(code);
    }

    int updated = repository.incrementClickCountByCode(code);
    if (updated == 0) {
      evictAfterCommit(code);
      throw new UrlNotFoundException(code);
    }
    evictAfterCommit(code);

    linkPublisher.publish(
        new LinkClickedEvent(
            code,
            now,
            source,
            context.clientIp(),
            context.userAgent(),
            context.referrer(),
            context.country()));

    log.info(
        "url.link.redirect.success code={} source={} targetHost={} clickCount={} clientIp={} country={} referrerPresent={} userAgentPresent={}",
        code,
        source,
        hostOf(snapshot.originalUrl()),
        snapshot.clickCount() + 1,
        maskIp(context.clientIp()),
        context.country(),
        context.referrer() != null && !context.referrer().isBlank(),
        context.userAgent() != null && !context.userAgent().isBlank());

    return snapshot.originalUrl();
  }

  private String hostOf(String url) {
    String host = URI.create(url).getHost();
    return host == null ? url : host;
  }

  private void evictAfterCommit(String code) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      cacheService.evict(code);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            cacheService.evict(code);
          }
        });
  }

  private String maskIp(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return "unknown";
    }
    if (ipAddress.contains(".")) {
      String[] parts = ipAddress.split("\\.");
      if (parts.length == 4) {
        return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
      }
    }
    if (ipAddress.contains(":")) {
      int index = ipAddress.indexOf(':');
      return index > 0 ? ipAddress.substring(0, index + 1) + "****" : "****";
    }
    return "masked";
  }
}
