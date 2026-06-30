package io.weblinkpilot.ai.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.weblinkpilot.ai.service.AiLinkMetadataService;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiLinkMetadataControllerStandaloneTest {

  @Mock private AiLinkMetadataService metadataService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AiLinkMetadataController(metadataService)).build();
  }

  @Test
  void returnsMetadataForPublicLink() throws Exception {
    when(metadataService.getByCode("redis")).thenReturn(response("redis"));

    mockMvc
        .perform(get("/api/v1/ai/links/redis/metadata"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("redis"))
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.category").value("Database"));

    verify(metadataService).getByCode("redis");
  }

  @Test
  void regeneratesMetadataForPublicLink() throws Exception {
    when(metadataService.regenerate("redis")).thenReturn(response("redis"));

    mockMvc
        .perform(post("/api/v1/ai/links/redis/metadata/regenerate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("redis"))
        .andExpect(jsonPath("$.status").value("READY"));

    verify(metadataService).regenerate("redis");
  }

  private static AiLinkMetadataResponse response(String code) {
    return new AiLinkMetadataResponse(
        code,
        "READY",
        "stub",
        "link-metadata-v1",
        "Redis",
        "Redis documentation.",
        "Database",
        List.of("redis", "cache"),
        "database",
        "redis",
        null,
        OffsetDateTime.now(ZoneOffset.UTC),
        OffsetDateTime.now(ZoneOffset.UTC));
  }
}
