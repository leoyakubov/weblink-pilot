package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

    private final ShortLinkRepository repository;
    private final UrlCacheService cacheService;
    private final LinkPublisher linkPublisher;

    public RedirectService(ShortLinkRepository repository, UrlCacheService cacheService, LinkPublisher linkPublisher) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.linkPublisher = linkPublisher;
    }

    @Transactional
    public String resolveTarget(String code, String ipAddress, String userAgent, String referrer) {
        ShortLinkSnapshot snapshot = cacheService.findByCode(code);
        if (snapshot == null) {
            throw new UrlNotFoundException(code);
        }

        ShortLink link = repository.findByCode(code).orElseThrow(() -> new UrlNotFoundException(code));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (link.isExpired(now)) {
            throw new UrlExpiredException(code);
        }

        link.incrementClickCount();
        repository.save(link);

        linkPublisher.publish(new LinkClickedEvent(
                code,
                now,
                ipAddress,
                userAgent,
                referrer
        ));

        return link.getOriginalUrl();
    }
}
