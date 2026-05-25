package io.weblinkpilot.url.service;

import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

    private final ShortLinkRepository repository;

    public UrlCacheService(ShortLinkRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "shortLinks", key = "#p0", unless = "#result == null")
    public ShortLinkSnapshot findByCode(String code) {
        log.debug("url.cache.lookup code={}", code);
        Optional<ShortLink> link = repository.findByCode(code);
        return link.map(value -> new ShortLinkSnapshot(
                value.getCode(),
                value.getOriginalUrl(),
                value.getOwnerUsername(),
                value.getCreatedAt(),
                value.getExpiresAt(),
                value.getClickCount()
        )).orElse(null);
    }

    @CacheEvict(cacheNames = "shortLinks", key = "#p0")
    public void evict(String code) {
        log.debug("url.cache.evict code={}", code);
    }
}
