package io.weblinkpilot.ai.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.domain.AiLinkMetadata;
import io.weblinkpilot.ai.domain.AiLinkMetadataStatus;
import io.weblinkpilot.ai.mapper.AiLinkMetadataMapper;
import io.weblinkpilot.ai.provider.AiLinkMetadataPrompt;
import io.weblinkpilot.ai.provider.AiProvider;
import io.weblinkpilot.ai.repository.AiLinkMetadataRepository;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import io.weblinkpilot.shared.ports.LinkAiMetadataService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring-managed configuration is intentionally retained by this service.")
public class AiLinkMetadataService implements LinkAiMetadataService {

  private static final Logger log = LoggerFactory.getLogger(AiLinkMetadataService.class);

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
          AiLinkMetadata.builder()
              .shortCode(event.code())
              .originalUrl(event.originalUrl())
              .status(AiLinkMetadataStatus.DISABLED)
              .provider(providerName())
              .promptVersion(properties.getPromptVersion())
              .now(now)
              .build());
      log.info("ai.metadata.disabled code={}", event.code());
      return;
    }

    AiLinkMetadata metadata =
        repository.save(
            AiLinkMetadata.builder()
                .shortCode(event.code())
                .originalUrl(event.originalUrl())
                .status(AiLinkMetadataStatus.PENDING)
                .provider(providerName())
                .promptVersion(properties.getPromptVersion())
                .now(now)
                .build());

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

  @Override
  @Transactional(readOnly = true)
  public Map<String, AiLinkMetadataResponse> metadataByCodes(Collection<String> codes) {
    List<String> normalizedCodes =
        codes == null
            ? List.of()
            : codes.stream().filter(code -> code != null && !code.isBlank()).distinct().toList();
    if (normalizedCodes.isEmpty()) {
      return Map.of();
    }

    return repository.findAllByShortCodeIn(normalizedCodes).stream()
        .map(mapper::toResponse)
        .collect(Collectors.toUnmodifiableMap(AiLinkMetadataResponse::code, Function.identity()));
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
    RuntimeException lastFailure = null;
    int attempts = Math.max(1, properties.getMaxAttempts());
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        generateOnce(metadata, customAlias);
        return;
      } catch (RuntimeException exception) {
        lastFailure = exception;
        log.warn(
            "ai.metadata.attempt_failed code={} attempt={} maxAttempts={} reason={}",
            metadata.getShortCode(),
            attempt,
            attempts,
            exception.getMessage());
      }
    }

    RuntimeException failure =
        lastFailure == null
            ? new IllegalStateException("AI metadata generation failed")
            : lastFailure;
    metadata.markFailed(safeMessage(failure), OffsetDateTime.now(ZoneOffset.UTC));
    log.warn("ai.metadata.failed code={} reason={}", metadata.getShortCode(), failure.getMessage());
  }

  private void generateOnce(AiLinkMetadata metadata, String customAlias) {
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
}
