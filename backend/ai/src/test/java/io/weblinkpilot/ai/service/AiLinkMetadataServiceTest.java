package io.weblinkpilot.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.ai.bootstrap.AiLinkMetadataBootstrapService;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import io.weblinkpilot.ai.mapper.AiLinkMetadataMapper;
import io.weblinkpilot.ai.provider.AiLinkMetadataPrompt;
import io.weblinkpilot.ai.provider.AiProvider;
import io.weblinkpilot.ai.provider.StubAiProvider;
import io.weblinkpilot.ai.repository.AiLinkMetadataRepository;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AiLinkMetadataServiceTest.TestConfig.class)
@Transactional
class AiLinkMetadataServiceTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan("io.weblinkpilot.ai.domain")
  @EnableJpaRepositories("io.weblinkpilot.ai.repository")
  @Import({
    AiLinkMetadataService.class,
    AiLinkMetadataMapper.class,
    AiLinkMetadataBootstrapService.class,
    StubAiProvider.class
  })
  static class TestConfig {

    @Bean
    FlakyAiProvider flakyAiProvider() {
      return new FlakyAiProvider();
    }
  }

  @Autowired private AiLinkMetadataService service;

  @Autowired private AiLinkMetadataBootstrapService bootstrapService;

  @Autowired private AiLinkMetadataRepository repository;

  @Autowired private FlakyAiProvider flakyAiProvider;

  @MockitoBean private AiProperties properties;

  @Test
  void enrichesLinkCreatedEventsWithStubMetadata() {
    when(properties.isEnabled()).thenReturn(true);
    when(properties.getProvider()).thenReturn("stub");
    when(properties.getPromptVersion()).thenReturn("link-metadata-v1");

    service.enrich(
        new LinkCreatedEvent(
            "redis",
            "https://redis.io/docs/latest/develop/",
            null,
            "user",
            OffsetDateTime.now(),
            null));

    var response = service.getByCode("redis");

    assertThat(response.status()).isEqualTo("READY");
    assertThat(response.provider()).isEqualTo("stub");
    assertThat(response.category()).isEqualTo("Programming");
    assertThat(response.tags()).contains("redis", "cache", "data");
    assertThat(repository.findByShortCode("redis")).isPresent();
  }

  @Test
  void storesDisabledStateWhenAiIsOff() {
    when(properties.isEnabled()).thenReturn(false);
    when(properties.getProvider()).thenReturn("stub");
    when(properties.getPromptVersion()).thenReturn("link-metadata-v1");

    service.enrich(
        new LinkCreatedEvent(
            "docs", "https://example.com/docs", null, null, OffsetDateTime.now(), null));

    assertThat(service.getByCode("docs").status()).isEqualTo("DISABLED");
  }

  @Test
  void seedsDefaultMetadataForDemoLinks() {
    when(properties.getProvider()).thenReturn("stub");
    when(properties.getPromptVersion()).thenReturn("link-metadata-v1");

    bootstrapService.seedDefaultMetadata();

    var response = service.getByCode("vue-js");

    assertThat(response.status()).isEqualTo("READY");
    assertThat(response.provider()).isEqualTo("seed");
    assertThat(response.title()).isEqualTo("Vue.js Guide");
    assertThat(response.category()).isEqualTo("Frontend");
    assertThat(response.tags()).contains("vue", "javascript", "frontend");
  }

  @Test
  void retriesProviderFailuresBeforeMarkingMetadataReady() {
    flakyAiProvider.reset();
    when(properties.isEnabled()).thenReturn(true);
    when(properties.getProvider()).thenReturn("flaky");
    when(properties.getPromptVersion()).thenReturn("link-metadata-v1");
    when(properties.getMaxAttempts()).thenReturn(2);

    service.enrich(
        new LinkCreatedEvent(
            "spring-boot",
            "https://spring.io/projects/spring-boot",
            null,
            null,
            OffsetDateTime.now(),
            null));

    var response = service.getByCode("spring-boot");

    assertThat(response.status()).isEqualTo("READY");
    assertThat(response.title()).isEqualTo("Recovered metadata");
    assertThat(flakyAiProvider.attempts()).isEqualTo(2);
  }

  @Test
  void returnsMetadataForMultipleCodesInOneLookup() {
    when(properties.getProvider()).thenReturn("stub");
    when(properties.getPromptVersion()).thenReturn("link-metadata-v1");

    bootstrapService.seedDefaultMetadata();

    var metadata = service.metadataByCodes(List.of("redis", "postgres", "missing", "redis"));

    assertThat(metadata).containsOnlyKeys("redis", "postgres");
    assertThat(metadata.get("redis").category()).isEqualTo("Database");
    assertThat(metadata.get("postgres").title()).isEqualTo("PostgreSQL");
  }

  static class FlakyAiProvider implements AiProvider {

    private final AtomicInteger attempts = new AtomicInteger();

    @Override
    public String name() {
      return "flaky";
    }

    @Override
    public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
      if (attempts.incrementAndGet() == 1) {
        throw new IllegalStateException("temporary provider failure");
      }

      return new AiLinkMetadataResult(
          "Recovered metadata",
          "Metadata generated after a transient failure.",
          "Programming",
          List.of("retry", "ai"),
          "code",
          "recovered-metadata");
    }

    int attempts() {
      return attempts.get();
    }

    void reset() {
      attempts.set(0);
    }
  }
}
