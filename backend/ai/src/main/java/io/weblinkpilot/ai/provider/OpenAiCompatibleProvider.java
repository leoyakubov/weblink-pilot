package io.weblinkpilot.ai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OpenAiCompatibleProvider implements AiProvider {

  private final AiProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public String name() {
    return "openai";
  }

  @Override
  public AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt) {
    if (properties.getOpenai().getApiKey() == null
        || properties.getOpenai().getApiKey().isBlank()) {
      throw new IllegalStateException("OpenAI-compatible provider requires an API key");
    }

    try {
      HttpRequest request =
          HttpRequest.newBuilder(endpoint())
              .timeout(timeout())
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
              .POST(HttpRequest.BodyPublishers.ofString(requestBody(prompt)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "OpenAI-compatible provider returned HTTP " + response.statusCode());
      }
      return parseResponse(response.body());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not call OpenAI-compatible provider", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OpenAI-compatible request interrupted", exception);
    }
  }

  private URI endpoint() {
    String baseUrl = properties.getOpenai().getBaseUrl().replaceAll("/+$", "");
    return URI.create(baseUrl + "/chat/completions");
  }

  private Duration timeout() {
    Duration timeout = properties.getOpenai().getTimeout();
    return timeout == null || timeout.isNegative() || timeout.isZero()
        ? Duration.ofSeconds(10)
        : timeout;
  }

  private String requestBody(AiLinkMetadataPrompt prompt) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        new ChatCompletionRequest(
            properties.getOpenai().getModel(),
            List.of(
                new Message("system", systemPrompt()),
                new Message(
                    "user",
                    "Link code: "
                        + prompt.code()
                        + "\nCurrent custom alias: "
                        + (prompt.customAlias() == null ? "" : prompt.customAlias())
                        + "\nTarget URL: "
                        + prompt.originalUrl())),
            new ResponseFormat("json_object"),
            0.2));
  }

  private String systemPrompt() {
    return String.join(
        System.lineSeparator(),
        "You enrich short links for WeblinkPilot.",
        "Return only valid JSON with these fields:",
        "title, summary, category, tags, icon, suggestedAlias.",
        "Keep values concise and product-friendly.",
        "Do not include markdown or any fields not requested.");
  }

  private AiLinkMetadataResult parseResponse(String body) throws JsonProcessingException {
    JsonNode root = objectMapper.readTree(body);
    JsonNode content = root.at("/choices/0/message/content");
    if (content.isMissingNode() || content.asText().isBlank()) {
      throw new IllegalStateException("OpenAI-compatible response did not include metadata");
    }

    JsonNode metadata = objectMapper.readTree(content.asText());
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

  private record ChatCompletionRequest(
      String model,
      List<Message> messages,
      @JsonProperty("response_format") ResponseFormat responseFormat,
      double temperature) {}

  private record Message(String role, String content) {}

  private record ResponseFormat(String type) {}
}
