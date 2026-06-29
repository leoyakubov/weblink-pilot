package io.weblinkpilot.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StubAiProviderTest {

  private final StubAiProvider provider = new StubAiProvider();

  @Test
  void generatesProgrammingMetadataForKnownTechnicalLinks() {
    var result =
        provider.generateLinkMetadata(
            new AiLinkMetadataPrompt(
                "spring-boot", "https://spring.io/projects/spring-boot", null));

    assertThat(result.title()).isEqualTo("Spring Boot");
    assertThat(result.category()).isEqualTo("Programming");
    assertThat(result.tags()).containsExactly("spring", "java", "backend");
    assertThat(result.suggestedAlias()).isEqualTo("spring-boot");
  }
}
