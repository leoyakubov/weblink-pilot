package io.weblinkpilot.ai.bootstrap;

import io.weblinkpilot.ai.domain.AiLinkMetadata;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import io.weblinkpilot.ai.domain.AiLinkMetadataStatus;
import io.weblinkpilot.ai.repository.AiLinkMetadataRepository;
import io.weblinkpilot.shared.seed.DemoSeedDataCatalog;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiLinkMetadataBootstrapService {

  private static final Logger log = LoggerFactory.getLogger(AiLinkMetadataBootstrapService.class);
  private static final String SEED_PROVIDER = "seed";
  private static final String SEED_PROMPT_VERSION = "seeded-link-metadata-v1";

  private final AiLinkMetadataRepository repository;

  public AiLinkMetadataBootstrapService(AiLinkMetadataRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void seedDefaultMetadata() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    for (DemoSeedDataCatalog.DemoLink seed : DemoSeedDataCatalog.links()) {
      AiLinkMetadata metadata =
          repository
              .findByShortCode(seed.code())
              .orElseGet(
                  () ->
                      AiLinkMetadata.builder()
                          .shortCode(seed.code())
                          .originalUrl(seed.originalUrl())
                          .status(AiLinkMetadataStatus.PENDING)
                          .provider(SEED_PROVIDER)
                          .promptVersion(SEED_PROMPT_VERSION)
                          .now(now)
                          .build());
      if (metadata.getStatus() == AiLinkMetadataStatus.READY) {
        continue;
      }
      metadata.markPending(SEED_PROVIDER, SEED_PROMPT_VERSION, now);
      metadata.markReady(toResult(seed.metadata()), now);
      repository.save(metadata);
      log.info("bootstrap.ai.metadata.seeded code={}", seed.code());
    }
  }

  private AiLinkMetadataResult toResult(DemoSeedDataCatalog.DemoLinkMetadata metadata) {
    return new AiLinkMetadataResult(
        metadata.title(),
        metadata.summary(),
        metadata.category(),
        metadata.tags(),
        metadata.icon(),
        metadata.suggestedAlias());
  }
}
