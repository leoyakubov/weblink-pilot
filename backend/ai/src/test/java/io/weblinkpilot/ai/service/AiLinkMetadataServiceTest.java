package io.weblinkpilot.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.provider.StubAiProvider;
import io.weblinkpilot.ai.repository.AiLinkMetadataRepository;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
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
  @Import({AiLinkMetadataService.class, AiLinkMetadataMapper.class, StubAiProvider.class})
  static class TestConfig {}

  @Autowired private AiLinkMetadataService service;

  @Autowired private AiLinkMetadataRepository repository;

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

    service.seedDefaultMetadata();

    var response = service.getByCode("vue-js");

    assertThat(response.status()).isEqualTo("READY");
    assertThat(response.provider()).isEqualTo("seed");
    assertThat(response.title()).isEqualTo("Vue.js Guide");
    assertThat(response.category()).isEqualTo("Frontend");
    assertThat(response.tags()).contains("vue", "javascript", "frontend");
  }
}
