package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlLookupService {

    private static final Logger log = LoggerFactory.getLogger(UrlLookupService.class);

    private final ShortLinkRepository repository;
    private final UrlCacheService cacheService;
    private final PublicUrlBuilder publicUrlBuilder;

    public UrlLookupService(ShortLinkRepository repository,
                            UrlCacheService cacheService,
                            PublicUrlBuilder publicUrlBuilder) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.publicUrlBuilder = publicUrlBuilder;
    }

    @Transactional(readOnly = true)
    public LinkResponse getByCode(String code) {
        ShortLinkSnapshot snapshot = cacheService.findByCode(code);
        if (snapshot == null) {
            log.warn("url.link.read.miss code={}", code);
            throw new UrlNotFoundException(code);
        }

        log.info(
                "url.link.read.success code={} clickCount={} originalHost={} expiresAt={}",
                snapshot.code(),
                snapshot.clickCount(),
                hostOf(snapshot.originalUrl()),
                snapshot.expiresAt()
        );
        return toResponse(snapshot);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listRecentLinks(int limit) {
        return listRecentLinks(null, false, limit);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listRecentLinks(String ownerUsername, boolean admin, int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        List<ShortLink> content;
        if (admin) {
            content = repository.findAll(PageRequest.of(0, size, newestFirst)).getContent();
        } else if (ownerUsername == null || ownerUsername.isBlank()) {
            content = repository.findAllByOwnerUsernameIsNull(PageRequest.of(0, size, newestFirst)).getContent();
        } else {
            content = repository.findAllByOwnerUsername(ownerUsername.trim().toLowerCase(java.util.Locale.ROOT), PageRequest.of(0, size, newestFirst)).getContent();
        }
        List<LinkResponse> links = content.stream()
                .map(this::toResponse)
                .toList();
        log.info("url.link.list.success limit={} returned={}", size, links.size());
        return links;
    }

    private LinkResponse toResponse(ShortLinkSnapshot snapshot) {
        return toResponse(
                snapshot.code(),
                snapshot.originalUrl(),
                snapshot.ownerUsername(),
                snapshot.createdAt(),
                snapshot.expiresAt(),
                snapshot.clickCount()
        );
    }

    private LinkResponse toResponse(ShortLink link) {
        return toResponse(
                link.getCode(),
                link.getOriginalUrl(),
                link.getOwnerUsername(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount()
        );
    }

    private LinkResponse toResponse(String code,
                                    String originalUrl,
                                    String ownerUsername,
                                    java.time.OffsetDateTime createdAt,
                                    java.time.OffsetDateTime expiresAt,
                                    long clickCount) {
        return new LinkResponse(
                code,
                publicUrlBuilder.buildShortUrl(code),
                publicUrlBuilder.buildQrCodeUrl(code),
                originalUrl,
                createdAt,
                expiresAt,
                clickCount,
                ownerUsername
        );
    }

    private String hostOf(String url) {
        String host = java.net.URI.create(url).getHost();
        return host == null ? url : host;
    }
}
