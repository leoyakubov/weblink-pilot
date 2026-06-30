package io.weblinkpilot.links.bootstrap;

import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.shared.seed.DemoSeedDataCatalog;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlBootstrapService {

  private static final Logger log = LoggerFactory.getLogger(UrlBootstrapService.class);

  private final ShortLinkRepository shortLinkRepository;

  public UrlBootstrapService(ShortLinkRepository shortLinkRepository) {
    this.shortLinkRepository = shortLinkRepository;
  }

  @Transactional
  public void seedDefaultLinks(String bootstrapUserUsername) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    String normalizedBootstrapUser = normalizeUsername(bootstrapUserUsername);
    for (DemoSeedDataCatalog.DemoLink seedLink : DemoSeedDataCatalog.links()) {
      seedLink(
          seedLink.code(),
          seedLink.originalUrl(),
          seedLink.customAlias(),
          ownerUsername(seedLink, normalizedBootstrapUser),
          now);
    }
  }

  private String ownerUsername(
      DemoSeedDataCatalog.DemoLink seedLink, String normalizedBootstrapUser) {
    return seedLink.ownerKind() == DemoSeedDataCatalog.OwnerKind.BOOTSTRAP_USER
        ? normalizedBootstrapUser
        : null;
  }

  private void seedLink(
      String code,
      String originalUrl,
      String customAlias,
      String ownerUsername,
      OffsetDateTime createdAt) {
    if (shortLinkRepository.existsByCode(code)) {
      return;
    }
    if (customAlias != null && shortLinkRepository.existsByCustomAlias(customAlias)) {
      return;
    }

    ShortLink link =
        ShortLink.builder()
            .code(code)
            .originalUrl(originalUrl)
            .customAlias(customAlias)
            .ownerUsername(ownerUsername)
            .createdAt(createdAt)
            .build();
    shortLinkRepository.save(link);
    log.info(
        "bootstrap.link.seeded code={} owner={}",
        code,
        ownerUsername == null ? "anonymous" : ownerUsername);
  }

  private String normalizeUsername(String username) {
    if (username == null) {
      return null;
    }
    String value = username.trim().toLowerCase(java.util.Locale.ROOT);
    return value.isBlank() ? null : value;
  }
}
