package io.weblinkpilot.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.weblinkpilot.ai.config.AiProperties;
import io.weblinkpilot.ai.domain.AiLinkMetadataResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification =
        "Spring-managed configuration and ObjectMapper beans are intentionally retained.")
public class OllamaProvider implements AiProvider {

  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OllamaProvider(AiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public String name() {
    return "ollama";
  }

  @Override
  public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(endpoint())
              .timeout(timeout())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody(prompt)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
      }
      return parseResponse(response.body());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not call Ollama", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Ollama request interrupted", exception);
    }
  }

  private URI endpoint() {
    String baseUrl = properties.getOllama().getBaseUrl().replaceAll("/+$", "");
    return URI.create(baseUrl + "/api/generate");
  }

  private Duration timeout() {
    Duration timeout = properties.getOllama().getTimeout();
    return timeout == null || timeout.isNegative() || timeout.isZero()
        ? Duration.ofSeconds(10)
        : timeout;
  }

  private String requestBody(AiLinkMetadataPrompt prompt) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        new OllamaGenerateRequest(
            properties.getOllama().getModel(),
            promptText(prompt),
            false,
            "json",
            new OllamaOptions(0.2)));
  }

  private String promptText(AiLinkMetadataPrompt prompt) {
    return String.join(
        System.lineSeparator(),
        "You enrich short links for WeblinkPilot.",
        "Return only valid JSON with these fields:",
        "title, summary, category, tags, icon, suggestedAlias.",
        "Rules:",
        "- title: human readable, max 80 characters",
        "- summary: one sentence, max 220 characters",
        "- category: short product-friendly category",
        "- tags: 2 to 5 lowercase tags",
        "- icon: one lowercase word such as link, docs, code, repo, video, product",
        "- suggestedAlias: lowercase URL alias, 3-64 chars, only letters, numbers, dash or underscore",
        "Do not include markdown.",
        "",
        "Link code: " + prompt.code(),
        "Current custom alias: " + (prompt.customAlias() == null ? "" : prompt.customAlias()),
        "Target URL: " + prompt.originalUrl());
  }

  private AiLinkMetadataResult parseResponse(String body) throws JsonProcessingException {
    JsonNode root = objectMapper.readTree(body);
    JsonNode response = root.get("response");
    if (response == null || response.asText().isBlank()) {
      throw new IllegalStateException("Ollama response did not include generated metadata");
    }

    JsonNode metadata = objectMapper.readTree(response.asText());
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

  private record OllamaGenerateRequest(
      String model, String prompt, boolean stream, String format, OllamaOptions options) {}

  private record OllamaOptions(double temperature) {}
}
