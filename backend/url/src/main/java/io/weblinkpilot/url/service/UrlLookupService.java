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
            log.warn("link.read.miss code={}", code);
            throw new UrlNotFoundException(code);
        }

        ShortLink link = repository.findByCode(code).orElseThrow(() -> new UrlNotFoundException(code));
        log.info(
                "link.read.success code={} clickCount={} originalHost={} expiresAt={}",
                link.getCode(),
                link.getClickCount(),
                hostOf(link.getOriginalUrl()),
                link.getExpiresAt()
        );
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listRecentLinks(int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        List<LinkResponse> links = repository.findAll(PageRequest.of(0, size, newestFirst))
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
        log.info("link.list.success limit={} returned={}", size, links.size());
        return links;
    }

    private LinkResponse toResponse(ShortLink link) {
        return new LinkResponse(
                link.getCode(),
                publicUrlBuilder.buildShortUrl(link.getCode()),
                publicUrlBuilder.buildQrCodeUrl(link.getCode()),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getClickCount()
        );
    }

    private String hostOf(String url) {
        String host = java.net.URI.create(url).getHost();
        return host == null ? url : host;
    }
}
