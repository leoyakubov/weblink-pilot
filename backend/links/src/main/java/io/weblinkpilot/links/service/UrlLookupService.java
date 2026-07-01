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
import io.weblinkpilot.shared.api.common.PaginatedResponse;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkAiMetadataService;
import io.weblinkpilot.shared.ports.LinkOwnerMetadataService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    return listRecentLinksPage(criteria).content();
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<LinkResponse> listRecentLinksPage(LinkSearchCriteria criteria) {
    int page = criteria.pageNumber();
    int size = criteria.pageSize(defaultBrowseLimit, maxBrowseLimit);
    PageRequest pageRequest = PageRequest.of(page, size, NEWEST_LINKS_FIRST);
    Page<ShortLink> pageResult = loadPage(criteria, pageRequest);
    List<LinkResponse> links = withAiMetadata(pageResult.getContent());
    log.info(
        "url.link.list.success page={} size={} returned={} total={}",
        page,
        size,
        links.size(),
        pageResult.getTotalElements());
    return PaginatedResponse.of(links, page, size, pageResult.getTotalElements());
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

  private Page<ShortLink> loadPage(LinkSearchCriteria criteria, PageRequest pageRequest) {
    List<String> roleUsernames = roleUsernames(criteria);
    if (criteria.admin() && criteria.ownerRole() != null && roleUsernames.isEmpty()) {
      return new PageImpl<>(List.of(), pageRequest, 0);
    }
    return repository.findAll(specification(criteria, roleUsernames), pageRequest);
  }

  private List<String> roleUsernames(LinkSearchCriteria criteria) {
    if (!criteria.admin()
        || criteria.ownerRole() == null
        || ANONYMOUS_ROLE.equalsIgnoreCase(criteria.ownerRole())) {
      return List.of();
    }

    return linkOwnerMetadataService.usernamesByRole(criteria.ownerRole()).stream()
        .map(String::trim)
        .map(username -> username.toLowerCase(Locale.ROOT))
        .filter(username -> !username.isBlank())
        .toList();
  }

  private Specification<ShortLink> specification(
      LinkSearchCriteria criteria, List<String> roleUsernames) {
    return Specification.allOf(
        notDeleted(),
        ownershipSpecification(criteria, roleUsernames),
        expirationSpecification(criteria.expiration()));
  }

  private Specification<ShortLink> notDeleted() {
    return (root, query, builder) -> builder.isNull(root.get("deletedAt"));
  }

  private Specification<ShortLink> ownershipSpecification(
      LinkSearchCriteria criteria, List<String> roleUsernames) {
    return (root, query, builder) -> {
      if (criteria.admin()) {
        if (criteria.creator() != null) {
          if (criteria.creatorIsAnonymous()) {
            return builder.isNull(root.get("ownerUsername"));
          }
          return builder.equal(root.get("ownerUsername"), criteria.creator());
        }
        if (ANONYMOUS_ROLE.equalsIgnoreCase(criteria.ownerRole())) {
          return builder.isNull(root.get("ownerUsername"));
        }
        if (criteria.ownerRole() != null) {
          return root.get("ownerUsername").in(roleUsernames);
        }
        return builder.conjunction();
      }

      if (criteria.ownerUsername() == null) {
        return builder.isNull(root.get("ownerUsername"));
      }
      return builder.equal(root.get("ownerUsername"), criteria.ownerUsername());
    };
  }

  private Specification<ShortLink> expirationSpecification(ExpirationFilter expirationFilter) {
    return (root, query, builder) -> {
      if (expirationFilter == null) {
        return builder.conjunction();
      }

      OffsetDateTime now = OffsetDateTime.now(clock);
      return switch (expirationFilter) {
        case ACTIVE ->
            builder.or(
                builder.isNull(root.get("expiresAt")),
                builder.greaterThan(root.get("expiresAt"), now));
        case EXPIRED ->
            builder.and(
                builder.isNotNull(root.get("expiresAt")),
                builder.lessThanOrEqualTo(root.get("expiresAt"), now));
        case NEVER -> builder.isNull(root.get("expiresAt"));
      };
    };
  }

  private String hostOf(String url) {
    String host = java.net.URI.create(url).getHost();
    return host == null ? url : host;
  }
}
