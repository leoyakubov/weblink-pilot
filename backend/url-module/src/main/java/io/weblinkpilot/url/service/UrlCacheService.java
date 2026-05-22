package io.weblinkpilot.url.service;

import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UrlCacheService {

    private final ShortLinkRepository repository;

    public UrlCacheService(ShortLinkRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "shortLinks", key = "#code", unless = "#result == null")
    public ShortLinkSnapshot findByCode(String code) {
        Optional<ShortLink> link = repository.findByCode(code);
        return link.map(value -> new ShortLinkSnapshot(
                value.getCode(),
                value.getOriginalUrl(),
                value.getCreatedAt(),
                value.getExpiresAt()
        )).orElse(null);
    }

    @CacheEvict(cacheNames = "shortLinks", key = "#code")
    public void evict(String code) {
        // cache eviction handled by annotation
    }
}
