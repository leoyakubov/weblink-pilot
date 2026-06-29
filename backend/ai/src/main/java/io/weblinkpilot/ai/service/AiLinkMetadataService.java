package io.weblinkpilot.ai.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.domain.AiLinkMetadata;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import io.weblinkpilot.ai.domain.AiLinkMetadataStatus;
import io.weblinkpilot.ai.provider.AiLinkMetadataPrompt;
import io.weblinkpilot.ai.provider.AiProvider;
import io.weblinkpilot.ai.repository.AiLinkMetadataRepository;
import io.weblinkpilot.shared.contracts.AiLinkMetadataResponse;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed configuration is intentionally retained by this service.")
public class AiLinkMetadataService {

  private static final Logger log = LoggerFactory.getLogger(AiLinkMetadataService.class);
  private static final String SEED_PROVIDER = "seed";
  private static final String SEED_PROMPT_VERSION = "seeded-link-metadata-v1";
  private static final List<SeedMetadata> DEFAULT_SEED_METADATA =
      List.of(
          new SeedMetadata(
              "spring-boot",
              "https://spring.io/projects/spring-boot",
              "Spring Boot",
              "Production-ready Java application framework for building APIs, services, and web applications with less setup.",
              "Programming",
              List.of("spring", "java", "backend"),
              "code",
              "spring-boot"),
          new SeedMetadata(
              "vue-js",
              "https://vuejs.org/guide/introduction.html",
              "Vue.js Guide",
              "Official Vue.js introduction for building reactive interfaces and modern frontend applications.",
              "Frontend",
              List.of("vue", "javascript", "frontend"),
              "sparkles",
              "vue-js"),
          new SeedMetadata(
              "postgres",
              "https://www.postgresql.org/about/",
              "PostgreSQL",
              "Overview of the open-source relational database used for reliable data storage and SQL workloads.",
              "Database",
              List.of("postgres", "database", "sql"),
              "database",
              "postgres"),
          new SeedMetadata(
              "redis",
              "https://redis.io/docs/latest/develop/",
              "Redis Developer Docs",
              "Redis documentation for fast data structures, caching, and application performance patterns.",
              "Database",
              List.of("redis", "cache", "data"),
              "database",
              "redis"));

  private final AiProperties properties;
  private final List<AiProvider> providers;
  private final AiLinkMetadataRepository repository;
  private final AiLinkMetadataMapper mapper;

  public AiLinkMetadataService(
      AiProperties properties,
      List<AiProvider> providers,
      AiLinkMetadataRepository repository,
      AiLinkMetadataMapper mapper) {
    this.properties = properties;
    this.providers = List.copyOf(providers);
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional
  public void enrich(LinkCreatedEvent event) {
    if (repository.existsByShortCode(event.code())) {
      log.debug("ai.metadata.skip reason=already_exists code={}", event.code());
      return;
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (!properties.isEnabled()) {
      repository.save(
          new AiLinkMetadata(
              event.code(),
              event.originalUrl(),
              AiLinkMetadataStatus.DISABLED,
              providerName(),
              properties.getPromptVersion(),
              now));
      log.info("ai.metadata.disabled code={}", event.code());
      return;
    }

    AiLinkMetadata metadata =
        repository.save(
            new AiLinkMetadata(
                event.code(),
                event.originalUrl(),
                AiLinkMetadataStatus.PENDING,
                providerName(),
                properties.getPromptVersion(),
                now));

    generate(metadata, event.customAlias());
  }

  @Transactional(readOnly = true)
  public AiLinkMetadataResponse getByCode(String code) {
    return repository
        .findByShortCode(code)
        .map(mapper::toResponse)
        .orElseGet(
            () ->
                new AiLinkMetadataResponse(
                    code,
                    "PENDING",
                    providerName(),
                    properties.getPromptVersion(),
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null));
  }

  @Transactional
  public void seedDefaultMetadata() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    for (SeedMetadata seed : DEFAULT_SEED_METADATA) {
      AiLinkMetadata metadata =
          repository
              .findByShortCode(seed.code())
              .orElseGet(
                  () ->
                      new AiLinkMetadata(
                          seed.code(),
                          seed.originalUrl(),
                          AiLinkMetadataStatus.PENDING,
                          SEED_PROVIDER,
                          SEED_PROMPT_VERSION,
                          now));
      if (metadata.getStatus() == AiLinkMetadataStatus.READY) {
        continue;
      }
      metadata.markPending(SEED_PROVIDER, SEED_PROMPT_VERSION, now);
      metadata.markReady(seed.toResult(), now);
      repository.save(metadata);
      log.info("bootstrap.ai.metadata.seeded code={}", seed.code());
    }
  }

  @Transactional
  public AiLinkMetadataResponse regenerate(String code) {
    AiLinkMetadata metadata =
        repository
            .findByShortCode(code)
            .orElseThrow(() -> new IllegalArgumentException("AI metadata not found: " + code));

    metadata.markPending(
        providerName(), properties.getPromptVersion(), OffsetDateTime.now(ZoneOffset.UTC));
    if (!properties.isEnabled()) {
      metadata.markFailed("AI enrichment is disabled", OffsetDateTime.now(ZoneOffset.UTC));
      log.info("ai.metadata.regenerate.disabled code={}", code);
      return mapper.toResponse(metadata);
    }

    generate(metadata, null);
    return mapper.toResponse(metadata);
  }

  private void generate(AiLinkMetadata metadata, String customAlias) {
    try {
      AiProvider provider = provider();
      metadata.markReady(
          provider.generateLinkMetadata(
              new AiLinkMetadataPrompt(
                  metadata.getShortCode(), metadata.getOriginalUrl(), customAlias)),
          OffsetDateTime.now(ZoneOffset.UTC));
      log.info(
          "ai.metadata.ready code={} provider={} category={}",
          metadata.getShortCode(),
          provider.name(),
          metadata.getCategory());
    } catch (RuntimeException exception) {
      metadata.markFailed(safeMessage(exception), OffsetDateTime.now(ZoneOffset.UTC));
      log.warn(
          "ai.metadata.failed code={} reason={}", metadata.getShortCode(), exception.getMessage());
    }
  }

  private AiProvider provider() {
    String configured = providerName();
    return providers.stream()
        .filter(provider -> configured.equalsIgnoreCase(provider.name()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Unsupported AI provider: " + configured));
  }

  private String providerName() {
    return properties.getProvider().trim().toLowerCase(Locale.ROOT);
  }

  private String safeMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }
    return message.length() > 1000 ? message.substring(0, 1000) : message;
  }

  private record SeedMetadata(
      String code,
      String originalUrl,
      String title,
      String summary,
      String category,
      List<String> tags,
      String icon,
      String suggestedAlias) {
    AiLinkMetadataResult toResult() {
      return new AiLinkMetadataResult(title, summary, category, tags, icon, suggestedAlias);
    }
  }
}
