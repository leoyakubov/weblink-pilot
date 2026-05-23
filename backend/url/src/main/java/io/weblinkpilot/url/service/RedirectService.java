package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.UrlExpiredException;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.web.RedirectRequestContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);

    private final ShortLinkRepository repository;
    private final UrlCacheService cacheService;
    private final LinkPublisher linkPublisher;

    public RedirectService(ShortLinkRepository repository, UrlCacheService cacheService, LinkPublisher linkPublisher) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.linkPublisher = linkPublisher;
    }

    @Transactional
    public String resolveTarget(String code, RedirectRequestContext context) {
        ShortLinkSnapshot snapshot = cacheService.findByCode(code);
        if (snapshot == null) {
            log.warn("link.redirect.miss code={}", code);
            throw new UrlNotFoundException(code);
        }

        ShortLink link = repository.findByCode(code).orElseThrow(() -> new UrlNotFoundException(code));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (link.isExpired(now)) {
            log.warn("link.redirect.expired code={} expiredAt={}", code, link.getExpiresAt());
            throw new UrlExpiredException(code);
        }

        link.incrementClickCount();
        repository.save(link);

        linkPublisher.publish(new LinkClickedEvent(
                code,
                now,
                context.clientIp(),
                context.userAgent(),
                context.referrer(),
                context.country()
        ));

        log.info(
                "link.redirect.success code={} targetHost={} clickCount={} clientIp={} country={} referrerPresent={} userAgentPresent={}",
                code,
                hostOf(link.getOriginalUrl()),
                link.getClickCount(),
                maskIp(context.clientIp()),
                context.country(),
                context.referrer() != null && !context.referrer().isBlank(),
                context.userAgent() != null && !context.userAgent().isBlank()
        );

        return link.getOriginalUrl();
    }

    private String hostOf(String url) {
        String host = URI.create(url).getHost();
        return host == null ? url : host;
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
