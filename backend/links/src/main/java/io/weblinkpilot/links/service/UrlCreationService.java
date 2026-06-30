package io.weblinkpilot.links.service;

import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.event.LinkPublisher;
import io.weblinkpilot.links.exception.DuplicateAliasException;
import io.weblinkpilot.links.mapper.LinkResponseMapper;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.links.validation.ShortLinkCreationValidator;
import io.weblinkpilot.shared.api.links.CreateLinkRequest;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlCreationService {

  private static final Logger log = LoggerFactory.getLogger(UrlCreationService.class);
  private static final String GENERATED_ALIAS_TYPE = "generated";
  private static final String CUSTOM_ALIAS_TYPE = "custom";

  private final ShortLinkRepository repository;
  private final ShortLinkCreationValidator creationValidator;
  private final ShortCodeAllocationStrategy codeAllocationStrategy;
  private final UrlCacheService cacheService;
  private final LinkPublisher linkPublisher;
  private final LinkResponseMapper responseMapper;

  public UrlCreationService(
      ShortLinkRepository repository,
      ShortLinkCreationValidator creationValidator,
      ShortCodeAllocationStrategy codeAllocationStrategy,
      UrlCacheService cacheService,
      LinkPublisher linkPublisher,
      LinkResponseMapper responseMapper) {
    this.repository = repository;
    this.creationValidator = creationValidator;
    this.codeAllocationStrategy = codeAllocationStrategy;
    this.cacheService = cacheService;
    this.linkPublisher = linkPublisher;
    this.responseMapper = responseMapper;
  }

  @Transactional
  public LinkResponse create(CreateLinkRequest request) {
    return create(request, null);
  }

  @Transactional
  public LinkResponse create(CreateLinkRequest request, String ownerUsername) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    ShortLinkCreationValidator.ValidatedCreation validated =
        creationValidator.validate(request, ownerUsername, now);
    String alias = validated.customAlias();
    ShortLink link =
        alias != null
            ? createWithCustomAlias(
                alias,
                validated.originalUrl(),
                validated.ownerUsername(),
                now,
                validated.expiresAt())
            : codeAllocationStrategy.createGeneratedLink(
                validated.originalUrl(), validated.ownerUsername(), now, validated.expiresAt());

    cacheService.evict(link.getCode());
    linkPublisher.publish(
        new LinkCreatedEvent(
            link.getCode(),
            link.getOriginalUrl(),
            link.getCustomAlias(),
            link.getOwnerUsername(),
            link.getCreatedAt(),
            link.getExpiresAt()));

    LinkResponse response = responseMapper.toResponse(link);
    log.info(
        "url.link.created code={} aliasType={} originalHost={} expiresAt={} shortUrl={}",
        link.getCode(),
        alias == null ? GENERATED_ALIAS_TYPE : CUSTOM_ALIAS_TYPE,
        hostOf(link.getOriginalUrl()),
        link.getExpiresAt(),
        response.shortUrl());

    return response;
  }

  private ShortLink createWithCustomAlias(
      String alias,
      String normalizedUrl,
      String ownerUsername,
      OffsetDateTime now,
      OffsetDateTime expiresAt) {
    if (repository.existsByCustomAlias(alias)) {
      log.warn("url.link.create.rejected reason=duplicate_alias alias={}", alias);
      throw new DuplicateAliasException(alias);
    }

    ShortLink link =
        ShortLink.builder()
            .code(alias)
            .originalUrl(normalizedUrl)
            .customAlias(alias)
            .ownerUsername(ownerUsername)
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();
    try {
      return repository.saveAndFlush(link);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateAliasException(alias);
    }
  }

  private String hostOf(String url) {
    String host = URI.create(url).getHost();
    return host == null ? url : host;
  }
}
