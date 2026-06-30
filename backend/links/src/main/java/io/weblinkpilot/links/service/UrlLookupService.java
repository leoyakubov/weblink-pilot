package io.weblinkpilot.links.service;

import io.weblinkpilot.links.cache.ShortLinkSnapshot;
import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.criteria.ExpirationFilter;
import io.weblinkpilot.links.criteria.LinkSearchCriteria;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.exception.UrlExpiredException;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.links.mapper.LinkResponseMapper;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.links.support.PublicUrlBuilder;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkAiMetadataService;
import io.weblinkpilot.shared.ports.LinkOwnerMetadataService;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlLookupService {

  private static final Logger log = LoggerFactory.getLogger(UrlLookupService.class);
  private static final Sort NEWEST_LINKS_FIRST =
      Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
  private static final String ANONYMOUS_ROLE = "ANONYMOUS";

  private final ShortLinkRepository repository;
  private final UrlCacheService cacheService;
  private final LinkOwnerMetadataService linkOwnerMetadataService;
  private final LinkAiMetadataService linkAiMetadataService;
  private final LinkResponseMapper responseMapper;
  private final Clock clock;
  private final int defaultBrowseLimit;
  private final int maxBrowseLimit;

  @Autowired
  public UrlLookupService(
      ShortLinkRepository repository,
      UrlCacheService cacheService,
      LinkOwnerMetadataService linkOwnerMetadataService,
      LinkAiMetadataService linkAiMetadataService,
      LinkResponseMapper responseMapper,
      ShortLinkProperties properties) {
    this.repository = repository;
    this.cacheService = cacheService;
    this.linkOwnerMetadataService = linkOwnerMetadataService;
    this.linkAiMetadataService = linkAiMetadataService;
    this.responseMapper = responseMapper;
    this.clock = Clock.systemUTC();
    this.defaultBrowseLimit = properties.getBrowse().getDefaultLimit();
    this.maxBrowseLimit = properties.getBrowse().getMaxLimit();
  }

  public UrlLookupService(
      ShortLinkRepository repository,
      UrlCacheService cacheService,
      LinkOwnerMetadataService linkOwnerMetadataService,
      LinkAiMetadataService linkAiMetadataService,
      LinkResponseMapper responseMapper) {
    this(
        repository,
        cacheService,
        linkOwnerMetadataService,
        linkAiMetadataService,
        responseMapper,
        new ShortLinkProperties());
  }

  public UrlLookupService(
      ShortLinkRepository repository,
      UrlCacheService cacheService,
      PublicUrlBuilder publicUrlBuilder) {
    this(
        repository,
        cacheService,
        new LinkOwnerMetadataService() {},
        new LinkAiMetadataService() {},
        new LinkResponseMapper(publicUrlBuilder, new LinkOwnerMetadataService() {}),
        new ShortLinkProperties());
  }

  @Transactional(readOnly = true)
  public LinkResponse getByCode(String code) {
    ShortLinkSnapshot snapshot = cacheService.findByCode(code);
    if (snapshot == null) {
      log.warn("url.link.read.miss code={}", code);
      throw new UrlNotFoundException(code);
    }
    if (snapshot.deletedAt() != null) {
      log.warn("url.link.read.expired code={} deletedAt={}", code, snapshot.deletedAt());
      throw new UrlExpiredException(code);
    }

    log.info(
        "url.link.read.success code={} clickCount={} originalHost={} expiresAt={}",
        snapshot.code(),
        snapshot.clickCount(),
        hostOf(snapshot.originalUrl()),
        snapshot.expiresAt());
    return responseMapper.toResponse(snapshot, metadataByCodes(List.of(snapshot.code())));
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(int limit) {
    return listRecentLinks(null, false, limit);
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(String ownerUsername, boolean admin, int limit) {
    return listRecentLinks(ownerUsername, admin, null, limit);
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(
      String ownerUsername, boolean admin, String creatorFilter, int limit) {
    return listRecentLinks(ownerUsername, admin, creatorFilter, null, limit);
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(
      String ownerUsername, boolean admin, String creatorFilter, String ownerRole, int limit) {
    return listRecentLinks(ownerUsername, admin, creatorFilter, ownerRole, null, limit);
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(
      String ownerUsername,
      boolean admin,
      String creatorFilter,
      String ownerRole,
      String expirationFilter,
      int limit) {
    return listRecentLinks(
        LinkSearchCriteria.user(
            ownerUsername, admin, creatorFilter, ownerRole, expirationFilter, limit));
  }

  @Transactional(readOnly = true)
  public List<LinkResponse> listRecentLinks(LinkSearchCriteria criteria) {
    int queryLimit = criteria.queryLimit(defaultBrowseLimit, maxBrowseLimit);
    int resultLimit = criteria.resultLimit(defaultBrowseLimit, maxBrowseLimit);
    List<ShortLink> content;
    if (criteria.admin() && criteria.creator() != null) {
      content = loadFilteredByCreator(criteria, queryLimit);
    } else if (criteria.admin() && criteria.ownerRole() != null) {
      content = loadFilteredByRole(criteria, queryLimit);
    } else if (criteria.admin()) {
      content =
          repository
              .findAllByDeletedAtIsNull(PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
              .getContent();
    } else if (criteria.ownerUsername() == null) {
      content =
          repository
              .findAllByOwnerUsernameIsNullAndDeletedAtIsNull(
                  PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
              .getContent();
    } else {
      content =
          repository
              .findAllByOwnerUsernameAndDeletedAtIsNull(
                  criteria.ownerUsername(), PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
              .getContent();
    }
    List<LinkResponse> links =
        withAiMetadata(
            content.stream()
                .filter(link -> matchesExpirationFilter(link, criteria.expiration()))
                .limit(resultLimit)
                .toList());
    log.info("url.link.list.success limit={} returned={}", resultLimit, links.size());
    return links;
  }

  private List<LinkResponse> withAiMetadata(List<ShortLink> links) {
    List<String> codes = links.stream().map(ShortLink::getCode).toList();
    Map<String, AiLinkMetadataResponse> metadata = metadataByCodes(codes);
    return links.stream().map(link -> responseMapper.toResponse(link, metadata)).toList();
  }

  private Map<String, AiLinkMetadataResponse> metadataByCodes(List<String> codes) {
    Map<String, AiLinkMetadataResponse> metadata = linkAiMetadataService.metadataByCodes(codes);
    return metadata == null ? Map.of() : metadata;
  }

  private List<ShortLink> loadFilteredByCreator(LinkSearchCriteria criteria, int queryLimit) {
    if (criteria.creatorIsAnonymous()) {
      return repository
          .findAllByOwnerUsernameIsNullAndDeletedAtIsNull(
              PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
          .getContent();
    }

    return repository
        .findAllByOwnerUsernameAndDeletedAtIsNull(
            criteria.creator(), PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
        .getContent();
  }

  private List<ShortLink> loadFilteredByRole(LinkSearchCriteria criteria, int queryLimit) {
    if (ANONYMOUS_ROLE.equalsIgnoreCase(criteria.ownerRole())) {
      return repository
          .findAllByOwnerUsernameIsNullAndDeletedAtIsNull(
              PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
          .getContent();
    }

    List<String> usernames =
        linkOwnerMetadataService.usernamesByRole(criteria.ownerRole()).stream()
            .map(String::trim)
            .map(username -> username.toLowerCase(Locale.ROOT))
            .filter(username -> !username.isBlank())
            .toList();
    if (usernames.isEmpty()) {
      return List.of();
    }

    return repository
        .findAllByOwnerUsernameInAndDeletedAtIsNull(
            usernames, PageRequest.of(0, queryLimit, NEWEST_LINKS_FIRST))
        .getContent();
  }

  private String hostOf(String url) {
    String host = java.net.URI.create(url).getHost();
    return host == null ? url : host;
  }

  private boolean matchesExpirationFilter(ShortLink link, ExpirationFilter expirationFilter) {
    if (expirationFilter == null) {
      return true;
    }
    return expirationFilter.matches(link, clock);
  }
}
