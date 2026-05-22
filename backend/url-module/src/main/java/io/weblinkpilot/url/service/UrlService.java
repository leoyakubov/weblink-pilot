package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private final ShortLinkRepository repository;
    private final Base62Codec base62Codec;
    private final UrlCacheService cacheService;
    private final LinkPublisher linkPublisher;

    public UrlService(ShortLinkRepository repository,
                      Base62Codec base62Codec,
                      UrlCacheService cacheService,
                      LinkPublisher linkPublisher) {
        this.repository = repository;
        this.base62Codec = base62Codec;
        this.cacheService = cacheService;
        this.linkPublisher = linkPublisher;
    }

    @Transactional
    public LinkResponse create(CreateLinkRequest request, String baseUrl) {
        String normalizedUrl = normalizeUrl(request.originalUrl());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (request.expiresAt() != null && request.expiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Expiration time must be in the future");
        }

        String alias = normalizeAlias(request.customAlias());
        if (alias != null && repository.existsByCustomAlias(alias)) {
            throw new DuplicateAliasException(alias);
        }

        String initialCode = alias != null ? alias : temporaryCode();
        ShortLink link = new ShortLink(initialCode, normalizedUrl, alias, now, request.expiresAt());
        try {
            link = repository.saveAndFlush(link);
        } catch (DataIntegrityViolationException ex) {
            if (alias != null) {
                throw new DuplicateAliasException(alias);
            }
            throw ex;
        }

        if (alias == null) {
            link.setCode(base62Codec.encode(link.getId()));
            link = repository.save(link);
        }

        cacheService.evict(link.getCode());
        linkPublisher.publish(new LinkCreatedEvent(
                link.getCode(),
                link.getOriginalUrl(),
                link.getCustomAlias(),
                link.getCreatedAt(),
                link.getExpiresAt()
        ));

        return new LinkResponse(
                link.getCode(),
                buildShortUrl(baseUrl, link.getCode()),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount()
        );
    }

    @Transactional(readOnly = true)
    public LinkResponse getByCode(String code, String baseUrl) {
        ShortLinkSnapshot snapshot = cacheService.findByCode(code);
        if (snapshot == null) {
            throw new UrlNotFoundException(code);
        }

        ShortLink link = repository.findByCode(code).orElseThrow(() -> new UrlNotFoundException(code));
        return new LinkResponse(
                link.getCode(),
                buildShortUrl(baseUrl, link.getCode()),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount()
        );
    }

    private String normalizeAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return null;
        }
        return customAlias.trim();
    }

    private String normalizeUrl(String rawUrl) {
        String value = rawUrl == null ? null : rawUrl.trim();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Original URL is required");
        }
        URI uri = URI.create(value);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("Original URL must be absolute and include scheme and host");
        }
        return uri.toString();
    }

    private String buildShortUrl(String baseUrl, String code) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/r/" + code;
    }

    private String temporaryCode() {
        return "tmp-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
