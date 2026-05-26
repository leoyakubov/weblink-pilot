package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.codegen.ShortCodeGenerator;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.DuplicateAliasException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlCreationService {

  private static final Logger log = LoggerFactory.getLogger(UrlCreationService.class);
  private static final int MAX_GENERATION_ATTEMPTS = 10;
  private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

  private final ShortLinkRepository repository;
  private final ShortCodeGenerator shortCodeGenerator;
  private final UrlCacheService cacheService;
  private final LinkPublisher linkPublisher;
  private final PublicUrlBuilder publicUrlBuilder;
  private final Duration maxExpiration;

  public UrlCreationService(
      ShortLinkRepository repository,
      ShortCodeGenerator shortCodeGenerator,
      UrlCacheService cacheService,
      LinkPublisher linkPublisher,
      PublicUrlBuilder publicUrlBuilder,
      @Value("${app.short-link.max-expiration:365d}") Duration maxExpiration) {
    this.repository = repository;
    this.shortCodeGenerator = shortCodeGenerator;
    this.cacheService = cacheService;
    this.linkPublisher = linkPublisher;
    this.publicUrlBuilder = publicUrlBuilder;
    this.maxExpiration = maxExpiration;
  }

  @Transactional
  public LinkResponse create(CreateLinkRequest request) {
    return create(request, null);
  }

  @Transactional
  public LinkResponse create(CreateLinkRequest request, String ownerUsername) {
    String normalizedUrl = normalizeUrl(request.originalUrl());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    String normalizedOwnerUsername = normalizeOwnerUsername(ownerUsername);
    if (request.expiresAt() != null) {
      validateExpirationWindow(request.expiresAt(), now);
    }

    String alias = normalizeAlias(request.customAlias());
    ShortLink link =
        alias != null
            ? createWithCustomAlias(
                alias, normalizedUrl, normalizedOwnerUsername, now, request.expiresAt())
            : createWithGeneratedCode(
                normalizedUrl, normalizedOwnerUsername, now, request.expiresAt());

    cacheService.evict(link.getCode());
    linkPublisher.publish(
        new LinkCreatedEvent(
            link.getCode(),
            link.getOriginalUrl(),
            link.getCustomAlias(),
            link.getOwnerUsername(),
            link.getCreatedAt(),
            link.getExpiresAt()));

    log.info(
        "url.link.created code={} aliasType={} originalHost={} expiresAt={} shortUrl={}",
        link.getCode(),
        alias == null ? "generated" : "custom",
        hostOf(link.getOriginalUrl()),
        link.getExpiresAt(),
        publicUrlBuilder.buildShortUrl(link.getCode()));

    return new LinkResponse(
        link.getCode(),
        publicUrlBuilder.buildShortUrl(link.getCode()),
        publicUrlBuilder.buildQrCodeUrl(link.getCode()),
        link.getOriginalUrl(),
        link.getCreatedAt(),
        link.getExpiresAt(),
        link.getClickCount(),
        link.getOwnerUsername());
  }

  private void validateExpirationWindow(OffsetDateTime expiresAt, OffsetDateTime now) {
    if (!expiresAt.isAfter(now)) {
      log.warn("url.link.create.rejected reason=expired_request expiresAt={}", expiresAt);
      throw new IllegalArgumentException("Expiration time must be in the future");
    }

    if (maxExpiration == null || maxExpiration.isNegative() || maxExpiration.isZero()) {
      return;
    }

    OffsetDateTime maxExpiresAt = now.plus(maxExpiration);
    if (expiresAt.isAfter(maxExpiresAt)) {
      log.warn(
          "url.link.create.rejected reason=expiration_too_far expiresAt={} maxExpiresAt={}",
          expiresAt,
          maxExpiresAt);
      throw new IllegalArgumentException("Expiration time exceeds the configured maximum lifetime");
    }
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

    ShortLink link = new ShortLink(alias, normalizedUrl, alias, ownerUsername, now, expiresAt);
    try {
      return repository.saveAndFlush(link);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateAliasException(alias);
    }
  }

  private ShortLink createWithGeneratedCode(
      String normalizedUrl, String ownerUsername, OffsetDateTime now, OffsetDateTime expiresAt) {
    for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
      String code = shortCodeGenerator.generate();
      if (repository.existsByCode(code)) {
        continue;
      }

      try {
        return repository.saveAndFlush(
            new ShortLink(code, normalizedUrl, null, ownerUsername, now, expiresAt));
      } catch (DataIntegrityViolationException exception) {
        if (attempt == MAX_GENERATION_ATTEMPTS) {
          throw exception;
        }
      }
    }

    throw new IllegalStateException("Unable to generate a unique short code");
  }

  private String normalizeAlias(String customAlias) {
    if (customAlias == null || customAlias.isBlank()) {
      return null;
    }
    String alias = customAlias.trim();
    if (!CUSTOM_ALIAS_PATTERN.matcher(alias).matches()) {
      throw new IllegalArgumentException(
          "Custom alias must be 3-64 characters long and contain only letters, digits, dash or underscore");
    }
    return alias;
  }

  private String normalizeUrl(String rawUrl) {
    String value = rawUrl == null ? null : rawUrl.trim();
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Original URL is required");
    }
    URI uri = URI.create(value);
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException(
          "Original URL must be absolute and include scheme and host");
    }
    return uri.toString();
  }

  private String normalizeOwnerUsername(String ownerUsername) {
    if (ownerUsername == null || ownerUsername.isBlank()) {
      return null;
    }
    return ownerUsername.trim().toLowerCase(Locale.ROOT);
  }

  private String hostOf(String url) {
    String host = URI.create(url).getHost();
    return host == null ? url : host;
  }
}
