package io.weblinkpilot.links.cache;

import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.shared.cache.CacheNames;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UrlCacheService {

  private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

  private final ShortLinkRepository repository;

  public UrlCacheService(ShortLinkRepository repository) {
    this.repository = repository;
  }

  @Cacheable(cacheNames = CacheNames.SHORT_LINKS, key = "#p0", unless = "#result == null")
  public ShortLinkSnapshot findByCode(String code) {
    log.debug("url.cache.lookup code={}", code);
    Optional<ShortLink> link = repository.findByCode(code);
    return link.map(
            value ->
                new ShortLinkSnapshot(
                    value.getCode(),
                    value.getOriginalUrl(),
                    value.getOwnerUsername(),
                    value.getCreatedAt(),
                    value.getExpiresAt(),
                    value.getDeletedAt(),
                    value.getClickCount()))
        .orElse(null);
  }

  @CacheEvict(cacheNames = CacheNames.SHORT_LINKS, key = "#p0")
  public void evict(String code) {
    log.debug("url.cache.evict code={}", code);
  }
}
