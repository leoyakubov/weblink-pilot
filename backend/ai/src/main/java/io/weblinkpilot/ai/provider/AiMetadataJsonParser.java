package io.weblinkpilot.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "A Spring-managed ObjectMapper is intentionally retained by this parser.")
public class AiMetadataJsonParser {

  private final ObjectMapper objectMapper;

  public AiMetadataJsonParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AiLinkMetadataResult parse(String metadataJson) throws JsonProcessingException {
    JsonNode metadata = objectMapper.readTree(metadataJson);
    return new AiLinkMetadataResult(
        text(metadata, "title"),
        text(metadata, "summary"),
        text(metadata, "category"),
        tags(metadata.get("tags")),
        text(metadata, "icon"),
        text(metadata, "suggestedAlias"));
  }

  private String text(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    return value == null || value.asText().isBlank() ? null : value.asText().trim();
  }

  private List<String> tags(JsonNode tagsNode) {
    if (tagsNode == null || !tagsNode.isArray()) {
      return List.of();
    }
    return StreamSupport.stream(tagsNode.spliterator(), false)
        .map(JsonNode::asText)
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .limit(5)
        .toList();
  }
}
