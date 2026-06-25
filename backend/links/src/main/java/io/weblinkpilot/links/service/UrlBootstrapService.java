package io.weblinkpilot.links.service;

import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
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
    seedLink("spring-boot", "https://spring.io/projects/spring-boot", null, null, now);
    seedLink("vue-js", "https://vuejs.org/guide/introduction.html", null, null, now);
    seedLink(
        "postgres",
        "https://www.postgresql.org/about/",
        null,
        normalizeUsername(bootstrapUserUsername),
        now);
    seedLink(
        "redis",
        "https://redis.io/docs/latest/develop/",
        null,
        normalizeUsername(bootstrapUserUsername),
        now);
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

    ShortLink link = new ShortLink(code, originalUrl, customAlias, ownerUsername, createdAt, null);
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
